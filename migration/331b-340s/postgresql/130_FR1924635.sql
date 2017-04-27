-- [ 1924635 ] Performance enhancement - Indexes on M_CostDetail

DROP INDEX M_COSTDETAIL_PRODUCT;

CREATE INDEX M_COSTDETAIL_PRODUCT ON M_COSTDETAIL (M_PRODUCT_ID, processed);

CREATE INDEX M_COSTDETAIL_ASI ON M_COSTDETAIL (M_AttributeSetInstance_ID);