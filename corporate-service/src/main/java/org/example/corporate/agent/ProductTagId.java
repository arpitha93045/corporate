package org.example.corporate.agent;

import java.io.Serializable;
import java.util.Objects;

public class ProductTagId implements Serializable {
    private Long productId;
    private String tag;

    public ProductTagId() {}

    public ProductTagId(Long productId, String tag) {
        this.productId = productId;
        this.tag = tag;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long v) { this.productId = v; }
    public String getTag() { return tag; }
    public void setTag(String v) { this.tag = v; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductTagId that)) return false;
        return Objects.equals(productId, that.productId) && Objects.equals(tag, that.tag);
    }

    @Override
    public int hashCode() { return Objects.hash(productId, tag); }
}
