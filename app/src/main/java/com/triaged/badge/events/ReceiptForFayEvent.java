package com.triaged.badge.events;

import com.triaged.badge.models.Receipt;

import java.util.List;

/**
 * Created by Sadegh Kazemy on 10/9/14.
 */
public class ReceiptForFayEvent {

    List<Receipt> receipts;

    public ReceiptForFayEvent(List<Receipt> receipts) {
        this.receipts = receipts;
    }

    public List<Receipt> getReceipts() {
        return receipts;
    }
}
