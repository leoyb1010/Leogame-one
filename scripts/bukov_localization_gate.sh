#!/usr/bin/env bash

set -u

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
repo_root="$(CDPATH= cd -- "$script_dir/.." && pwd)"

if ! command -v node >/dev/null 2>&1; then
	echo "ERROR: Node.js is required to run the Bukov localization gate." >&2
	exit 2
fi

exec node - "$repo_root" <<'NODE'
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(process.argv[2]);
const checks = [];

function relative(file) {
	return path.relative(root, file).split(path.sep).join('/');
}

function lineNumber(text, index) {
	let line = 1;
	for (let cursor = 0; cursor < index; cursor++) {
		if (text.charCodeAt(cursor) === 10) line++;
	}
	return line;
}

function addCheck(name, issues) {
	checks.push({name, issues});
}

function unescapeProperty(value) {
	let result = '';
	for (let i = 0; i < value.length; i++) {
		if (value[i] !== '\\' || i + 1 >= value.length) {
			result += value[i];
			continue;
		}
		const next = value[++i];
		if (next === 't') result += '\t';
		else if (next === 'n') result += '\n';
		else if (next === 'r') result += '\r';
		else if (next === 'f') result += '\f';
		else if (next === 'u' && /^[0-9a-fA-F]{4}$/.test(
				value.slice(i + 1, i + 5))) {
			result += String.fromCharCode(
					parseInt(value.slice(i + 1, i + 5), 16));
			i += 4;
		} else {
			result += next;
		}
	}
	return result;
}

function trailingBackslashes(value) {
	let count = 0;
	for (let i = value.length - 1; i >= 0 && value[i] === '\\'; i--) {
		count++;
	}
	return count;
}

function propertySeparator(line) {
	let escaped = false;
	for (let i = 0; i < line.length; i++) {
		const character = line[i];
		if (escaped) {
			escaped = false;
			continue;
		}
		if (character === '\\') {
			escaped = true;
			continue;
		}
		if (character === '=' || character === ':'
				|| /\s/.test(character)) {
			return i;
		}
	}
	return line.length;
}

function parseProperties(file) {
	const text = fs.readFileSync(file, 'utf8').replace(/\r\n?/g, '\n');
	const physical = text.split('\n');
	const logical = [];
	for (let i = 0; i < physical.length; i++) {
		const startLine = i + 1;
		let line = physical[i];
		while (trailingBackslashes(line) % 2 === 1
				&& i + 1 < physical.length) {
			line = line.slice(0, -1) + physical[++i].replace(/^\s+/, '');
		}
		logical.push({line, lineNumber: startLine});
	}

	const values = new Map();
	const firstLines = new Map();
	const duplicates = [];
	for (const record of logical) {
		const trimmed = record.line.replace(/^\s+/, '');
		if (!trimmed || trimmed[0] === '#' || trimmed[0] === '!') continue;

		const separator = propertySeparator(trimmed);
		let valueStart = separator;
		if (valueStart < trimmed.length && /\s/.test(trimmed[valueStart])) {
			while (valueStart < trimmed.length
					&& /\s/.test(trimmed[valueStart])) valueStart++;
			if (trimmed[valueStart] === '=' || trimmed[valueStart] === ':') {
				valueStart++;
			}
		} else if (valueStart < trimmed.length) {
			valueStart++;
		}
		while (valueStart < trimmed.length
				&& /\s/.test(trimmed[valueStart])) valueStart++;

		const key = unescapeProperty(trimmed.slice(0, separator));
		const value = unescapeProperty(trimmed.slice(valueStart));
		if (values.has(key)) {
			duplicates.push(
					`${relative(file)}:${record.lineNumber}: duplicate key `
					+ `'${key}' (first defined at line ${firstLines.get(key)})`);
		} else {
			values.set(key, value);
			firstLines.set(key, record.lineNumber);
		}
	}
	return {values, duplicates};
}

function multiset(values) {
	const result = new Map();
	for (const value of values) {
		result.set(value, (result.get(value) || 0) + 1);
	}
	return result;
}

function sortedMultiset(value) {
	return [...value.entries()]
			.sort(([left], [right]) => left.localeCompare(right))
			.map(([token, count]) => `${token}×${count}`)
			.join(', ');
}

function formatPlaceholders(value) {
	const tokens = [];
	const malformed = [];
	const pattern =
			/%(?:(\d+)\$)?([-#+ 0,(<]*)(\d*)(?:\.(\d+))?([tT])?([a-zA-Z%])/g;
	let implicitIndex = 1;
	let previousIndex = null;
	let match;
	const coveredPercents = new Set();
	while ((match = pattern.exec(value)) !== null) {
		for (let offset = 0; offset < match[0].length; offset++) {
			if (match[0][offset] === '%') {
				coveredPercents.add(match.index + offset);
			}
		}
		const conversion = match[6];
		if (conversion === '%' || conversion.toLowerCase() === 'n') {
			tokens.push(`noarg:${conversion.toLowerCase()}`);
			continue;
		}
		let argumentIndex;
		if (match[1]) {
			argumentIndex = Number(match[1]);
		} else if (match[2].includes('<')) {
			argumentIndex = previousIndex;
			if (argumentIndex === null) {
				malformed.push(`${match[0]} reuses a missing previous argument`);
				continue;
			}
		} else {
			argumentIndex = implicitIndex++;
		}
		previousIndex = argumentIndex;
		const type = (match[5] || '').toLowerCase()
				+ conversion.toLowerCase();
		tokens.push(`arg:${argumentIndex}:${type}`);
	}
	for (let i = 0; i < value.length; i++) {
		if (value[i] === '%' && !coveredPercents.has(i)) {
			malformed.push(`unrecognized format sequence at character ${i + 1}`);
		}
	}
	return {tokens: multiset(tokens), malformed};
}

function hasCjk(value) {
	return /[\u2E80-\u9FFF\uF900-\uFAFF\u3040-\u30FF\uAC00-\uD7AF]/u
			.test(value);
}

function walkJava(directory, result) {
	if (!fs.existsSync(directory)) return;
	for (const entry of fs.readdirSync(directory, {withFileTypes: true})) {
		const file = path.join(directory, entry.name);
		if (entry.isDirectory()) walkJava(file, result);
		else if (entry.isFile() && entry.name.endsWith('.java')) {
			result.push(file);
		}
	}
}

function cjkJavaStringLiterals(file) {
	const text = fs.readFileSync(file, 'utf8');
	const issues = [];
	let state = 'code';
	let start = -1;
	let literal = '';
	for (let i = 0; i < text.length; i++) {
		const current = text[i];
		const next = text[i + 1];
		if (state === 'code') {
			if (current === '/' && next === '/') {
				state = 'line-comment';
				i++;
			} else if (current === '/' && next === '*') {
				state = 'block-comment';
				i++;
			} else if (current === '"') {
				state = 'string';
				start = i;
				literal = '';
			} else if (current === '\'') {
				state = 'char';
			}
		} else if (state === 'line-comment') {
			if (current === '\n') state = 'code';
		} else if (state === 'block-comment') {
			if (current === '*' && next === '/') {
				state = 'code';
				i++;
			}
		} else if (state === 'char') {
			if (current === '\\') i++;
			else if (current === '\'') state = 'code';
		} else if (state === 'string') {
			if (current === '\\') {
				const unicode = text.slice(i + 1).match(/^u+([0-9a-fA-F]{4})/);
				if (unicode) {
					literal += String.fromCharCode(parseInt(unicode[1], 16));
					i += unicode[0].length;
				} else if (i + 1 < text.length) {
					literal += current + next;
					i++;
				}
			} else if (current === '"') {
				if (hasCjk(literal)) {
					const preview = literal.replace(/\s+/g, ' ').slice(0, 100);
					issues.push(
							`${relative(file)}:${lineNumber(text, start)}: `
							+ `hardcoded CJK string "${preview}"`);
				}
				state = 'code';
			} else {
				literal += current;
			}
		}
	}
	return issues;
}

function stripJavaComments(text) {
	let result = '';
	let state = 'code';
	for (let i = 0; i < text.length; i++) {
		const current = text[i];
		const next = text[i + 1];
		if (state === 'code') {
			if (current === '/' && next === '/') {
				result += '  ';
				state = 'line-comment';
				i++;
			} else if (current === '/' && next === '*') {
				result += '  ';
				state = 'block-comment';
				i++;
			} else if (current === '"') {
				result += current;
				state = 'string';
			} else if (current === '\'') {
				result += current;
				state = 'char';
			} else {
				result += current;
			}
		} else if (state === 'line-comment') {
			if (current === '\n') {
				result += '\n';
				state = 'code';
			} else {
				result += ' ';
			}
		} else if (state === 'block-comment') {
			if (current === '*' && next === '/') {
				result += '  ';
				state = 'code';
				i++;
			} else {
				result += current === '\n' ? '\n' : ' ';
			}
		} else if (state === 'string') {
			result += current;
			if (current === '\\' && i + 1 < text.length) {
				result += text[++i];
			} else if (current === '"') {
				state = 'code';
			}
		} else if (state === 'char') {
			result += current;
			if (current === '\\' && i + 1 < text.length) {
				result += text[++i];
			} else if (current === '\'') {
				state = 'code';
			}
		}
	}
	return result;
}

console.log('Bukov localization gate');
console.log(`Repository: ${root}`);

const bundles = ['bukov_entry', 'bukov_raid', 'bukov_economy'];
const parsedBundles = new Map();
const parityIssues = [];
const placeholderIssues = [];
for (const bundle of bundles) {
	const directory = path.join(
			root, 'core/src/main/assets/messages', bundle);
	const englishFile = path.join(directory, `${bundle}.properties`);
	const chineseFile = path.join(directory, `${bundle}_zh.properties`);
	for (const file of [englishFile, chineseFile]) {
		if (!fs.existsSync(file)) {
			parityIssues.push(`${relative(file)}: missing resource file`);
		}
	}
	if (!fs.existsSync(englishFile) || !fs.existsSync(chineseFile)) continue;

	const english = parseProperties(englishFile);
	const chinese = parseProperties(chineseFile);
	parsedBundles.set(bundle, {englishFile, chineseFile, english, chinese});
	parityIssues.push(...english.duplicates, ...chinese.duplicates);

	const englishKeys = new Set(english.values.keys());
	const chineseKeys = new Set(chinese.values.keys());
	for (const key of [...englishKeys].sort()) {
		if (!chineseKeys.has(key)) {
			parityIssues.push(
					`${relative(chineseFile)}: missing key '${key}'`);
		}
	}
	for (const key of [...chineseKeys].sort()) {
		if (!englishKeys.has(key)) {
			parityIssues.push(
					`${relative(englishFile)}: missing key '${key}'`);
		}
	}

	for (const key of [...englishKeys].filter(
			key => chineseKeys.has(key)).sort()) {
		const englishFormat = formatPlaceholders(english.values.get(key));
		const chineseFormat = formatPlaceholders(chinese.values.get(key));
		for (const issue of englishFormat.malformed) {
			placeholderIssues.push(
					`${relative(englishFile)}: key '${key}': ${issue}`);
		}
		for (const issue of chineseFormat.malformed) {
			placeholderIssues.push(
					`${relative(chineseFile)}: key '${key}': ${issue}`);
		}
		const englishSignature = sortedMultiset(englishFormat.tokens);
		const chineseSignature = sortedMultiset(chineseFormat.tokens);
		if (englishSignature !== chineseSignature) {
			placeholderIssues.push(
					`${bundle}: key '${key}' placeholder mismatch: `
					+ `EN [${englishSignature}] vs ZH [${chineseSignature}]`);
		}
	}
}
addCheck('EN/ZH key parity and duplicate-key check', parityIssues);
addCheck('Java String.format placeholder multiset parity',
		placeholderIssues);

const cjkResourceIssues = [];
const exactCjkAllowlist = new Set([
	'bukov.entry.brand.chinese_title',
	'bukov.entry.brand.bilingual_logo',
	'bukov.entry.reserved.bilingual_logo'
]);
for (const [bundle, parsed] of parsedBundles.entries()) {
	for (const [key, value] of parsed.english.values.entries()) {
		if (hasCjk(value) && !exactCjkAllowlist.has(key)) {
			cjkResourceIssues.push(
					`${relative(parsed.englishFile)}: key '${key}' `
					+ 'contains CJK outside the exact brand allowlist');
		}
	}
	if (bundle !== 'bukov_entry') continue;
	for (const key of exactCjkAllowlist) {
		const value = parsed.english.values.get(key);
		if (value === undefined) {
			cjkResourceIssues.push(
					`${relative(parsed.englishFile)}: allowlisted key `
					+ `'${key}' is missing`);
		} else if (!hasCjk(value)) {
			cjkResourceIssues.push(
					`${relative(parsed.englishFile)}: allowlisted key `
					+ `'${key}' no longer contains CJK; remove stale exception`);
		}
	}
}
addCheck('English resources contain no non-brand CJK',
		cjkResourceIssues);

const playerJava = [];
walkJava(path.join(
		root,
		'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/bukov'),
		playerJava);
for (const scene of [
	'TitleScene.java',
	'WelcomeScene.java',
	'BukovHubScene.java',
	'BukovDeploymentScene.java'
]) {
	const file = path.join(
			root,
			'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes',
			scene);
	if (fs.existsSync(file)) playerJava.push(file);
}
const uniquePlayerJava = [...new Set(playerJava)].sort();
const javaCjkIssues = [];
for (const file of uniquePlayerJava) {
	javaCjkIssues.push(...cjkJavaStringLiterals(file));
}
addCheck('Player-path Java contains no hardcoded CJK strings',
		javaCjkIssues);

const allMainJava = [];
walkJava(path.join(root, 'core/src/main/java'), allMainJava);
const directMessageIssues = [];
const directPattern =
		/\bMessages\s*\.\s*get\s*\(\s*"bukov\.[^"]*/g;
for (const file of allMainJava.sort()) {
	const raw = fs.readFileSync(file, 'utf8');
	const source = stripJavaComments(raw);
	let match;
	while ((match = directPattern.exec(source)) !== null) {
		directMessageIssues.push(
				`${relative(file)}:${lineNumber(source, match.index)}: `
				+ `direct ${match[0]}; use BukovMessages.get`);
	}
}
addCheck('Bukov keys use BukovMessages instead of Messages directly',
		directMessageIssues);

let failureCount = 0;
for (const check of checks) {
	if (check.issues.length === 0) {
		console.log(`[PASS] ${check.name}`);
		continue;
	}
	failureCount += check.issues.length;
	console.log(`[FAIL] ${check.name} (${check.issues.length})`);
	for (const issue of check.issues) console.log(`  - ${issue}`);
}

if (failureCount === 0) {
	console.log(`RESULT: PASS (${checks.length} checks)`);
	process.exit(0);
}
console.log(
		`RESULT: FAIL (${failureCount} issue`
		+ `${failureCount === 1 ? '' : 's'} across `
		+ `${checks.filter(check => check.issues.length > 0).length} checks)`);
process.exit(1);
NODE
