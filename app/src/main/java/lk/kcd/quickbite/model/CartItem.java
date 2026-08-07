package lk.kcd.quickbite.model;


import com.google.firebase.firestore.Exclude;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Getter (onMethod_ = {@Exclude})
    @Setter(onMethod_ = {@Exclude})


private String documentId;
private     String productId;

private int quantity;

private List<Attribute> attributes;

    public CartItem(String productId, int quantity, List<Attribute> attributes) {
        this.productId = productId;
        this.quantity = quantity;
        this.attributes = attributes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
public static class  Attribute{
    private  String name;
    private  String value;

}
}
