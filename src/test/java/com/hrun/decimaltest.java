package com.hrun;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.math.BigDecimal;

public class decimaltest {
    private static final BigDecimal LEVEL1_ORDER_PRICE_LEFT_MARGIN = new BigDecimal(0);
    private static final BigDecimal LEVEL2_ORDER_PRICE_LEFT_MARGIN = new BigDecimal(0.02);
    private static final BigDecimal LEVEL2_ORDER_PRICE_RIGHT_MARGIN = new BigDecimal(100);

    @Test
    public void test1(){
        /*public static int evalPotentialOrderLevel() {
            // 公益订单 - 支援疫情的类型
            String charityLevel = pOrder.getCharityLevel();
            if(StringUtils.isNotEmpty(charityLevel) && charityLevels.containsKey(charityLevel.trim())) {
                return 3;
            }

            BigDecimal price = pOrder.getPrice();
            int level = 0;
            if(price.compareTo(LEVEL1_ORDER_PRICE_LEFT_MARGIN) > 0
                    && price.compareTo(LEVEL2_ORDER_PRICE_LEFT_MARGIN) < 0) {
                level = 1;
            } else if(price.compareTo(LEVEL2_ORDER_PRICE_LEFT_MARGIN) >= 0
                    && price.compareTo(LEVEL2_ORDER_PRICE_RIGHT_MARGIN) <= 0
                    || (pOrder.getSwitchAutoAssign() != null && pOrder.getSwitchAutoAssign() == 1)) {
                level = 2;
            } else if(price.compareTo(LEVEL2_ORDER_PRICE_RIGHT_MARGIN) > 0) {
                level = 2;
            }
            return level;
        }*/
    }
}
