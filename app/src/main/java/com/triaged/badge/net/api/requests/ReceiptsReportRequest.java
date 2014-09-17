package com.triaged.badge.net.api.requests;

import com.triaged.badge.models.Receipt;

import java.util.List;

/**
 * Created by Sadegh Kazemy on 9/16/14.
 */
public class ReceiptsReportRequest {


    List<Receipt> receipts;

    public ReceiptsReportRequest(List<Receipt> receiptList) {
        this.receipts = receiptList;
    }
}
