package lk.kcd.quickbite.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;
import java.util.List;

public class Order {

    private String orderId;
    private String userId;
    private double totalAmount;
    private String status;
    private Timestamp orderDate;
    private List<OrderItem> orderItems;
    private Address shippingAddress;
    private Address billingAddress;

    public Order() {}


    @PropertyName("orderDate")
    public Timestamp getOrderDate() { return orderDate; }

    @PropertyName("orderDate")
    public void setOrderDate(Object raw) {
        if (raw instanceof Timestamp) {
            this.orderDate = (Timestamp) raw;
        } else if (raw instanceof Long) {
            this.orderDate = new Timestamp(new Date((Long) raw));
        } else if (raw instanceof Date) {
            this.orderDate = new Timestamp((Date) raw);
        } else {
            this.orderDate = null;
        }
    }

    // ── Getters & Setters ─────────────────────────────────────────────────
    public String getOrderId()                       { return orderId; }
    public void setOrderId(String v)                 { this.orderId = v; }
    public String getUserId()                        { return userId; }
    public void setUserId(String v)                  { this.userId = v; }
    public double getTotalAmount()                   { return totalAmount; }
    public void setTotalAmount(double v)             { this.totalAmount = v; }
    public String getStatus()                        { return status; }
    public void setStatus(String v)                  { this.status = v; }
    public List<OrderItem> getOrderItems()           { return orderItems; }
    public void setOrderItems(List<OrderItem> v)     { this.orderItems = v; }
    public Address getShippingAddress()              { return shippingAddress; }
    public void setShippingAddress(Address v)        { this.shippingAddress = v; }
    public Address getBillingAddress()               { return billingAddress; }
    public void setBillingAddress(Address v)         { this.billingAddress = v; }


    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Order o = new Order();
        public Builder orderId(String v)             { o.orderId = v; return this; }
        public Builder userId(String v)              { o.userId = v; return this; }
        public Builder totalAmount(double v)         { o.totalAmount = v; return this; }
        public Builder status(String v)              { o.status = v; return this; }
        public Builder orderDate(Timestamp v)        { o.orderDate = v; return this; }
        public Builder orderItems(List<OrderItem> v) { o.orderItems = v; return this; }
        public Builder shippingAddress(Address v)    { o.shippingAddress = v; return this; }
        public Builder billingAddress(Address v)     { o.billingAddress = v; return this; }
        public Order build()                         { return o; }
    }


    public static class OrderItem {
        private String productId;
        private double unitPrice;
        private int quantity;
        private List<Attribute> attributes;
        public OrderItem() {}
        public String getProductId()               { return productId; }
        public void setProductId(String v)         { this.productId = v; }
        public double getUnitPrice()               { return unitPrice; }
        public void setUnitPrice(double v)         { this.unitPrice = v; }
        public int getQuantity()                   { return quantity; }
        public void setQuantity(int v)             { this.quantity = v; }
        public List<Attribute> getAttributes()     { return attributes; }
        public void setAttributes(List<Attribute> v){ this.attributes = v; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final OrderItem i = new OrderItem();
            public Builder productId(String v)           { i.productId = v; return this; }
            public Builder unitPrice(double v)           { i.unitPrice = v; return this; }
            public Builder quantity(int v)               { i.quantity = v; return this; }
            public Builder attributes(List<Attribute> v) { i.attributes = v; return this; }
            public OrderItem build()                     { return i; }
        }

        public static class Attribute {
            private String name, value;
            public Attribute() {}
            public Attribute(String name, String value) { this.name = name; this.value = value; }
            public String getName()        { return name; }
            public void setName(String v)  { this.name = v; }
            public String getValue()       { return value; }
            public void setValue(String v) { this.value = v; }
            public static Builder builder() { return new Builder(); }
            public static class Builder {
                private final Attribute a = new Attribute();
                public Builder name(String v)  { a.name = v; return this; }
                public Builder value(String v) { a.value = v; return this; }
                public Attribute build()       { return a; }
            }
        }
    }


    public static class Address {
        private String name, email, contact, address1, address2, city, postcode;
        public Address() {}
        public String getName()           { return name; }
        public void setName(String v)     { this.name = v; }
        public String getEmail()          { return email; }
        public void setEmail(String v)    { this.email = v; }
        public String getContact()        { return contact; }
        public void setContact(String v)  { this.contact = v; }
        public String getAddress1()       { return address1; }
        public void setAddress1(String v) { this.address1 = v; }
        public String getAddress2()       { return address2; }
        public void setAddress2(String v) { this.address2 = v; }
        public String getCity()           { return city; }
        public void setCity(String v)     { this.city = v; }
        public String getPostcode()       { return postcode; }
        public void setPostcode(String v) { this.postcode = v; }
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final Address a = new Address();
            public Builder name(String v)     { a.name = v; return this; }
            public Builder email(String v)    { a.email = v; return this; }
            public Builder contact(String v)  { a.contact = v; return this; }
            public Builder address1(String v) { a.address1 = v; return this; }
            public Builder address2(String v) { a.address2 = v; return this; }
            public Builder city(String v)     { a.city = v; return this; }
            public Builder postcode(String v) { a.postcode = v; return this; }
            public Address build()            { return a; }
        }
    }
}