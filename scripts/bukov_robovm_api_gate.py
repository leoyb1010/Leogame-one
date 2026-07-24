#!/usr/bin/env python3
"""Checks Bukov bytecode method references against the RoboVM runtime JAR.

Usage:
  scripts/bukov_robovm_api_gate.py [robovm-rt.jar] [compiled-classes-dir]

The gate uses only Python's standard library. It reads class-file constant
pools and method tables directly, so it does not depend on the host JDK's
bootstrap classes or on javap resolving java.* names from the wrong runtime.
"""

from __future__ import annotations

import glob
import os
import subprocess
import struct
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple


DEFAULT_JAR_GLOB = os.path.expanduser(
    "~/.gradle/caches/modules-2/files-2.1/com.mobidevelop.robovm/"
    "robovm-rt/2.3.24/*/robovm-rt-2.3.24.jar"
)
PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CLASSES = PROJECT_ROOT / "core/build/classes/java/main"
DEFAULT_SOURCES = PROJECT_ROOT / "core/src/main/java"
BUKOV_PREFIX = (
    "com/shatteredpixel/shatteredpixeldungeon/bukov/"
)
JAVA_PREFIXES = ("java/lang/", "java/util/")


@dataclass(frozen=True, order=True)
class Method:
    owner: str
    name: str
    descriptor: str

    def display(self) -> str:
        return f"{self.owner}.{self.name}{self.descriptor}"


@dataclass
class ClassInfo:
    name: str
    superclass: Optional[str]
    interfaces: Tuple[str, ...]
    methods: Set[Tuple[str, str]]
    references: Set[Method]
    major_version: int


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.offset = 0

    def u1(self) -> int:
        value = self.data[self.offset]
        self.offset += 1
        return value

    def u2(self) -> int:
        value = struct.unpack_from(">H", self.data, self.offset)[0]
        self.offset += 2
        return value

    def u4(self) -> int:
        value = struct.unpack_from(">I", self.data, self.offset)[0]
        self.offset += 4
        return value

    def skip(self, length: int) -> None:
        self.offset += length


def parse_class(data: bytes) -> ClassInfo:
    reader = Reader(data)
    if reader.u4() != 0xCAFEBABE:
        raise ValueError("not a Java class file")
    reader.u2()
    major = reader.u2()
    count = reader.u2()
    pool: List[Optional[Tuple]] = [None] * count
    index = 1
    while index < count:
        tag = reader.u1()
        if tag == 1:
            length = reader.u2()
            raw = data[reader.offset : reader.offset + length]
            reader.skip(length)
            pool[index] = (tag, raw.decode("utf-8", errors="replace"))
        elif tag in (3, 4):
            reader.skip(4)
            pool[index] = (tag,)
        elif tag in (5, 6):
            reader.skip(8)
            pool[index] = (tag,)
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, reader.u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag, reader.u2(), reader.u2())
        elif tag == 15:
            pool[index] = (tag, reader.u1(), reader.u2())
        else:
            raise ValueError(f"unsupported constant-pool tag {tag}")
        index += 1

    def utf8(pool_index: int) -> str:
        entry = pool[pool_index]
        if entry is None or entry[0] != 1:
            raise ValueError(f"constant #{pool_index} is not UTF-8")
        return entry[1]

    def class_name(pool_index: int) -> str:
        entry = pool[pool_index]
        if entry is None or entry[0] != 7:
            raise ValueError(f"constant #{pool_index} is not a class")
        return utf8(entry[1])

    references: Set[Method] = set()
    for entry in pool:
        if entry is None or entry[0] not in (10, 11):
            continue
        owner = class_name(entry[1])
        name_and_type = pool[entry[2]]
        if name_and_type is None or name_and_type[0] != 12:
            raise ValueError("method reference has invalid name-and-type")
        references.add(
            Method(
                owner,
                utf8(name_and_type[1]),
                utf8(name_and_type[2]),
            )
        )

    reader.u2()
    this_class = reader.u2()
    super_class = reader.u2()
    interface_count = reader.u2()
    interfaces = tuple(
        class_name(reader.u2()) for _ in range(interface_count)
    )

    def skip_members() -> None:
        member_count = reader.u2()
        for _ in range(member_count):
            reader.u2()
            reader.u2()
            reader.u2()
            attribute_count = reader.u2()
            for _ in range(attribute_count):
                reader.u2()
                reader.skip(reader.u4())

    skip_members()
    method_count = reader.u2()
    methods: Set[Tuple[str, str]] = set()
    for _ in range(method_count):
        reader.u2()
        name = utf8(reader.u2())
        descriptor = utf8(reader.u2())
        methods.add((name, descriptor))
        attribute_count = reader.u2()
        for _ in range(attribute_count):
            reader.u2()
            reader.skip(reader.u4())

    return ClassInfo(
        name=class_name(this_class),
        superclass=class_name(super_class) if super_class else None,
        interfaces=interfaces,
        methods=methods,
        references=references,
        major_version=major,
    )


class RuntimeSurface:
    def __init__(self, jar_path: Path):
        self.archive = zipfile.ZipFile(jar_path)
        self.cache: Dict[str, Optional[ClassInfo]] = {}

    def close(self) -> None:
        self.archive.close()

    def class_info(self, name: str) -> Optional[ClassInfo]:
        if name in self.cache:
            return self.cache[name]
        try:
            data = self.archive.read(name + ".class")
        except KeyError:
            self.cache[name] = None
            return None
        info = parse_class(data)
        self.cache[name] = info
        return info

    def resolves(self, method: Method) -> bool:
        if (
            method.owner == "java/lang/invoke/MethodHandle"
            and method.name in ("invoke", "invokeExact")
        ):
            return self.class_info(method.owner) is not None
        return self._resolves(
            method.owner,
            method.name,
            method.descriptor,
            set(),
        )

    def _resolves(
        self,
        owner: str,
        name: str,
        descriptor: str,
        visited: Set[str],
    ) -> bool:
        if owner in visited:
            return False
        visited.add(owner)
        info = self.class_info(owner)
        if info is None:
            return False
        if (name, descriptor) in info.methods:
            return True
        parents: Iterable[str] = (
            tuple([info.superclass]) if info.superclass else tuple()
        ) + info.interfaces
        return any(
            self._resolves(parent, name, descriptor, visited)
            for parent in parents
        )


CAPABILITY_PROBES: Sequence[Tuple[str, Method]] = (
    (
        "Float.isFinite",
        Method("java/lang/Float", "isFinite", "(F)Z"),
    ),
    (
        "Math.addExact(int,int)",
        Method("java/lang/Math", "addExact", "(II)I"),
    ),
    (
        "Math.addExact(long,long)",
        Method("java/lang/Math", "addExact", "(JJ)J"),
    ),
    (
        "Math.floorMod(int,int)",
        Method("java/lang/Math", "floorMod", "(II)I"),
    ),
    (
        "Math.floorMod(long,long)",
        Method("java/lang/Math", "floorMod", "(JJ)J"),
    ),
    (
        "Long.compareUnsigned",
        Method("java/lang/Long", "compareUnsigned", "(JJ)I"),
    ),
    (
        "Long.divideUnsigned",
        Method("java/lang/Long", "divideUnsigned", "(JJ)J"),
    ),
    (
        "Long.remainderUnsigned",
        Method("java/lang/Long", "remainderUnsigned", "(JJ)J"),
    ),
    (
        "Long.parseUnsignedLong(String)",
        Method(
            "java/lang/Long",
            "parseUnsignedLong",
            "(Ljava/lang/String;)J",
        ),
    ),
    (
        "Long.parseUnsignedLong(String,int)",
        Method(
            "java/lang/Long",
            "parseUnsignedLong",
            "(Ljava/lang/String;I)J",
        ),
    ),
    (
        "Long.toUnsignedString(long)",
        Method(
            "java/lang/Long",
            "toUnsignedString",
            "(J)Ljava/lang/String;",
        ),
    ),
    (
        "Long.toUnsignedString(long,int)",
        Method(
            "java/lang/Long",
            "toUnsignedString",
            "(JI)Ljava/lang/String;",
        ),
    ),
    (
        "List.sort",
        Method(
            "java/util/List",
            "sort",
            "(Ljava/util/Comparator;)V",
        ),
    ),
    (
        "String.join(varargs)",
        Method(
            "java/lang/String",
            "join",
            "(Ljava/lang/CharSequence;"
            "[Ljava/lang/CharSequence;)Ljava/lang/String;",
        ),
    ),
    (
        "String.join(iterable)",
        Method(
            "java/lang/String",
            "join",
            "(Ljava/lang/CharSequence;"
            "Ljava/lang/Iterable;)Ljava/lang/String;",
        ),
    ),
)


def discover_jar(argument: Optional[str]) -> Path:
    if argument:
        path = Path(argument).expanduser().resolve()
        if not path.is_file():
            raise SystemExit(f"RoboVM runtime JAR not found: {path}")
        return path
    matches = sorted(glob.glob(DEFAULT_JAR_GLOB))
    if len(matches) != 1:
        raise SystemExit(
            "expected exactly one RoboVM 2.3.24 runtime JAR; "
            f"found {len(matches)}. Pass its path explicitly."
        )
    return Path(matches[0]).resolve()


def discover_classes(argument: Optional[str]) -> Path:
    if argument:
        path = Path(argument).expanduser().resolve()
        production_classes(path)
        return path

    candidates: List[Path] = []
    configured_root = os.environ.get("APPLE_BUILD_ROOT")
    if configured_root:
        candidates.append(
            Path(configured_root).expanduser().resolve()
            / "core/classes/java/main"
        )
    try:
        darwin_cache = subprocess.check_output(
            ["getconf", "DARWIN_USER_CACHE_DIR"],
            text=True,
        ).strip()
    except (FileNotFoundError, subprocess.CalledProcessError):
        darwin_cache = ""
    if darwin_cache:
        candidates.append(
            Path(darwin_cache).resolve()
            / "escape-from-bukov-gradle/core/classes/java/main"
        )
    candidates.append(DEFAULT_CLASSES.resolve())

    unique_candidates: List[Path] = []
    for candidate in candidates:
        if candidate not in unique_candidates:
            unique_candidates.append(candidate)

    rejected: List[str] = []
    for candidate in unique_candidates:
        try:
            production_classes(candidate)
        except SystemExit as error:
            rejected.append(f"{candidate}: {error}")
            continue
        freshness = stale_or_missing_classes(
            candidate,
            DEFAULT_SOURCES,
        )
        if not freshness:
            return candidate
        rejected.append(
            f"{candidate}: {len(freshness)} stale or missing classes"
        )
    rendered = "\n  ".join(rejected)
    raise SystemExit(
        "fresh compiled Bukov classes were not found in any supported build "
        f"directory:\n  {rendered}\n"
        "compile core classes before running the gate"
    )


def production_classes(directory: Path) -> List[Path]:
    root = directory / BUKOV_PREFIX
    if not root.is_dir():
        raise SystemExit(
            f"compiled Bukov classes not found under {root}; "
            "compile core classes before running the gate"
        )
    class_files = sorted(root.rglob("*.class"))
    if not class_files:
        raise SystemExit(
            f"compiled Bukov class directory is empty: {root}; "
            "compile core classes before running the gate"
        )
    return class_files


def stale_or_missing_classes(
    classes_dir: Path,
    sources_dir: Path,
) -> List[str]:
    source_root = sources_dir / BUKOV_PREFIX
    if not source_root.is_dir():
        return []
    problems: List[str] = []
    for source in sorted(source_root.rglob("*.java")):
        relative = source.relative_to(sources_dir).with_suffix(".class")
        compiled = classes_dir / relative
        if not compiled.is_file():
            problems.append(f"missing {relative}")
        elif compiled.stat().st_mtime_ns < source.stat().st_mtime_ns:
            problems.append(f"stale {relative}")
    return problems


def audit(jar_path: Path, classes_dir: Path) -> int:
    class_files = production_classes(classes_dir)
    freshness = stale_or_missing_classes(
        classes_dir,
        DEFAULT_SOURCES,
    )
    if freshness:
        print(
            "Compiled Bukov bytecode is incomplete or older than source; "
            "compile core classes before trusting the API gate:"
        )
        for problem in freshness:
            print(f"  {problem}")
        return 2

    references: Set[Method] = set()
    users: Dict[Method, Set[str]] = {}
    highest_major = 0
    for class_file in class_files:
        info = parse_class(class_file.read_bytes())
        highest_major = max(highest_major, info.major_version)
        relevant = {
            method
            for method in info.references
            if method.owner.startswith(JAVA_PREFIXES)
        }
        references.update(relevant)
        for method in relevant:
            users.setdefault(method, set()).add(info.name)

    surface = RuntimeSurface(jar_path)
    try:
        print(f"RoboVM runtime: {jar_path}")
        print(f"Bukov classes: {len(class_files)}")
        print(f"Highest Bukov class-file major: {highest_major}")
        print(f"Unique java.lang/java.util method references: {len(references)}")
        print("Capability probes:")
        for label, method in CAPABILITY_PROBES:
            state = "present" if surface.resolves(method) else "MISSING"
            print(f"  {state:7} {label}: {method.display()}")

        missing = sorted(
            method for method in references if not surface.resolves(method)
        )
        if missing:
            print(
                "Missing method references used by compiled Bukov "
                f"production classes ({len(missing)}):"
            )
            for method in missing:
                print(f"  {method.display()}")
                for user in sorted(users[method]):
                    print(f"    referenced by {user}")
            return 1

        print(
            "PASS: every compiled Bukov java.lang/java.util method "
            "reference resolves in robovm-rt."
        )
        return 0
    finally:
        surface.close()


def main(arguments: Sequence[str]) -> int:
    if len(arguments) > 2:
        raise SystemExit(
            "usage: bukov_robovm_api_gate.py "
            "[robovm-rt.jar] [compiled-classes-dir]"
        )
    jar_path = discover_jar(arguments[0] if arguments else None)
    classes_dir = discover_classes(
        arguments[1] if len(arguments) == 2 else None
    )
    return audit(jar_path, classes_dir)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
