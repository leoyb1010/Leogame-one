package com.shatteredpixel.shatteredpixeldungeon.bukov.raid;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/** Durable result of one vendor command, used to make retries idempotent. */
public final class BukovEconomyReceipt implements Bundlable {

	public enum Operation {
		BUY,
		SELL
	}

	private static final String TRANSACTION_ID = "transaction_id";
	private static final String OPERATION = "operation";
	private static final String SUBJECT_ID = "subject_id";
	private static final String ITEM_UID = "item_uid";
	private static final String CURRENCY_DELTA = "currency_delta";
	private static final String BALANCE_AFTER = "balance_after";

	private String transactionId;
	private Operation operation;
	private String subjectId;
	private String itemUid;
	private long currencyDelta;
	private long balanceAfter;

	public BukovEconomyReceipt() {
		// Required by Bundle reflection.
	}

	BukovEconomyReceipt(
			String transactionId,
			Operation operation,
			String subjectId,
			String itemUid,
			long currencyDelta,
			long balanceAfter) {
		this.transactionId = requireId(transactionId, "transactionId");
		if (operation == null) {
			throw new IllegalArgumentException("operation is required");
		}
		this.operation = operation;
		this.subjectId = requireId(subjectId, "subjectId");
		this.itemUid = requireId(itemUid, "itemUid");
		this.currencyDelta = currencyDelta;
		if (balanceAfter < 0L) {
			throw new IllegalArgumentException(
					"balanceAfter must be non-negative");
		}
		this.balanceAfter = balanceAfter;
	}

	String transactionId() {
		return transactionId;
	}

	Operation operation() {
		return operation;
	}

	String subjectId() {
		return subjectId;
	}

	String itemUid() {
		return itemUid;
	}

	long currencyDelta() {
		return currencyDelta;
	}

	long balanceAfter() {
		return balanceAfter;
	}

	BukovEconomyReceipt copy() {
		return new BukovEconomyReceipt(
				transactionId,
				operation,
				subjectId,
				itemUid,
				currencyDelta,
				balanceAfter);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(TRANSACTION_ID, transactionId);
		bundle.put(OPERATION, operation);
		bundle.put(SUBJECT_ID, subjectId);
		bundle.put(ITEM_UID, itemUid);
		bundle.put(CURRENCY_DELTA, currencyDelta);
		bundle.put(BALANCE_AFTER, balanceAfter);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		String restoredTransactionId = requireId(
				bundle.getString(TRANSACTION_ID),
				"transactionId");
		Operation restoredOperation =
				bundle.getEnum(OPERATION, Operation.class);
		if (restoredOperation == null) {
			throw new IllegalStateException(
					"Economy receipt operation is required");
		}
		String restoredSubjectId = requireId(
				bundle.getString(SUBJECT_ID),
				"subjectId");
		String restoredItemUid = requireId(
				bundle.getString(ITEM_UID),
				"itemUid");
		long restoredBalance = bundle.getLong(BALANCE_AFTER);
		if (restoredBalance < 0L) {
			throw new IllegalStateException(
					"Economy receipt balance cannot be negative");
		}
		transactionId = restoredTransactionId;
		operation = restoredOperation;
		subjectId = restoredSubjectId;
		itemUid = restoredItemUid;
		currencyDelta = bundle.getLong(CURRENCY_DELTA);
		balanceAfter = restoredBalance;
	}

	private static String requireId(String value, String name) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(name + " is required");
		}
		return value;
	}
}
