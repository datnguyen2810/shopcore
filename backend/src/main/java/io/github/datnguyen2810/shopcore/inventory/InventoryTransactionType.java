package io.github.datnguyen2810.shopcore.inventory;

public enum InventoryTransactionType {
    IMPORT,
    RESERVE, // lưu trữ sau khi customer đặt chờ thanh toan
    RELEASE, // trả hàng về kho (đơn bị hủy, quá hạn thanh toán)
    SALE,
    ADJUST // điều chỉnh kho
}
