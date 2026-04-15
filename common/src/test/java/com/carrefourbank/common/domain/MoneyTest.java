package com.carrefourbank.common.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    // ─── Factory / guards ────────────────────────────────────────────────────

    @Test
    void of_throwsNullPointerException_whenAmountIsNull() {
        assertThrows(NullPointerException.class, () -> Money.of(null, Currency.BRL));
    }

    @Test
    void of_throwsNullPointerException_whenCurrencyIsNull() {
        assertThrows(NullPointerException.class, () -> Money.of(new BigDecimal("100"), (Currency) null));
    }

    @Test
    void ofBRL_createsMoney_withBRLCurrency() {
        Money money = Money.ofBRL(new BigDecimal("50.00"));
        assertEquals(Currency.BRL, money.currency());
        assertEquals(0, new BigDecimal("50.00").compareTo(money.amount()));
    }

    @Test
    void ofUSD_createsMoney_withUSDCurrency() {
        Money money = Money.ofUSD(new BigDecimal("30.00"));
        assertEquals(Currency.USD, money.currency());
    }

    @Test
    void zero_createsMoney_withZeroAmount() {
        Money money = Money.zero(Currency.BRL);
        assertTrue(money.isZero());
        assertEquals(Currency.BRL, money.currency());
    }

    // ─── Sign predicates ─────────────────────────────────────────────────────

    @Test
    void isZero_returnsTrue_whenAmountIsZero() {
        assertTrue(Money.zero(Currency.BRL).isZero());
    }

    @Test
    void isZero_returnsFalse_whenAmountIsNonZero() {
        assertFalse(Money.ofBRL(new BigDecimal("0.01")).isZero());
    }

    @Test
    void isPositive_returnsTrue_whenAmountIsPositive() {
        assertTrue(Money.ofBRL(new BigDecimal("100.00")).isPositive());
    }

    @Test
    void isPositive_returnsFalse_whenAmountIsNegative() {
        assertFalse(Money.ofBRL(new BigDecimal("-1.00")).isPositive());
    }

    @Test
    void isNegative_returnsTrue_whenAmountIsNegative() {
        assertTrue(Money.ofBRL(new BigDecimal("-50.00")).isNegative());
    }

    @Test
    void isNegative_returnsFalse_whenAmountIsPositive() {
        assertFalse(Money.ofBRL(new BigDecimal("50.00")).isNegative());
    }

    // ─── add ─────────────────────────────────────────────────────────────────

    @Test
    void add_returnsSumOfAmounts() {
        Money a = Money.ofBRL(new BigDecimal("100.00"));
        Money b = Money.ofBRL(new BigDecimal("30.00"));
        Money result = a.add(b);
        assertEquals(0, new BigDecimal("130.00").compareTo(result.amount()));
        assertEquals(Currency.BRL, result.currency());
    }

    @Test
    void add_throwsIllegalArgumentException_whenCurrenciesDiffer() {
        Money brl = Money.ofBRL(new BigDecimal("100.00"));
        Money usd = Money.ofUSD(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> brl.add(usd));
    }

    // ─── subtract ────────────────────────────────────────────────────────────

    @Test
    void subtract_returnsDifference() {
        Money a = Money.ofBRL(new BigDecimal("100.00"));
        Money b = Money.ofBRL(new BigDecimal("30.00"));
        Money result = a.subtract(b);
        assertEquals(0, new BigDecimal("70.00").compareTo(result.amount()));
    }

    @Test
    void subtract_throwsIllegalArgumentException_whenCurrenciesDiffer() {
        Money brl = Money.ofBRL(new BigDecimal("100.00"));
        Money usd = Money.ofUSD(new BigDecimal("50.00"));
        assertThrows(IllegalArgumentException.class, () -> brl.subtract(usd));
    }

    // ─── multiply ────────────────────────────────────────────────────────────

    @Test
    void multiply_returnsProduct() {
        Money money = Money.ofBRL(new BigDecimal("100.00"));
        Money result = money.multiply(new BigDecimal("2"));
        assertEquals(0, new BigDecimal("200.00").compareTo(result.amount()));
    }

    @Test
    void multiply_throwsNullPointerException_whenFactorIsNull() {
        assertThrows(NullPointerException.class, () -> Money.ofBRL(new BigDecimal("100")).multiply(null));
    }

    // ─── divide ──────────────────────────────────────────────────────────────

    @Test
    void divide_returnsQuotient_forTerminatingDecimal() {
        Money money = Money.ofBRL(new BigDecimal("100.00"));
        Money result = money.divide(new BigDecimal("4"));
        assertEquals(0, new BigDecimal("25.00").compareTo(result.amount().setScale(2)));
    }

    @Test
    void divide_returnsQuotient_forNonTerminatingDecimal() {
        // 100 / 3 = 33.333... — previously threw ArithmeticException
        Money money = Money.ofBRL(new BigDecimal("100.00"));
        assertDoesNotThrow(() -> {
            Money result = money.divide(new BigDecimal("3"));
            assertTrue(result.isPositive());
        });
    }

    @Test
    void divide_throwsArithmeticException_whenDivisorIsZero() {
        assertThrows(ArithmeticException.class,
                () -> Money.ofBRL(new BigDecimal("100")).divide(BigDecimal.ZERO));
    }

    @Test
    void divide_throwsNullPointerException_whenDivisorIsNull() {
        assertThrows(NullPointerException.class,
                () -> Money.ofBRL(new BigDecimal("100")).divide(null));
    }

    // ─── negate / abs ────────────────────────────────────────────────────────

    @Test
    void negate_returnsNegatedValue() {
        Money positive = Money.ofBRL(new BigDecimal("100.00"));
        Money negated = positive.negate();
        assertTrue(negated.isNegative());
        assertEquals(0, new BigDecimal("-100.00").compareTo(negated.amount()));
    }

    @Test
    void abs_returnsAbsoluteValue_forNegative() {
        Money negative = Money.ofBRL(new BigDecimal("-75.00"));
        Money absolute = negative.abs();
        assertTrue(absolute.isPositive());
        assertEquals(0, new BigDecimal("75.00").compareTo(absolute.amount()));
    }

    @Test
    void abs_returnsAbsoluteValue_forPositive() {
        Money positive = Money.ofBRL(new BigDecimal("75.00"));
        assertEquals(0, positive.amount().compareTo(positive.abs().amount()));
    }

    // ─── equals / hashCode contract ──────────────────────────────────────────

    @Test
    void equals_returnsTrue_whenAmountsAreEqualByCompareTo_differentScale() {
        Money a = Money.ofBRL(new BigDecimal("100"));
        Money b = Money.ofBRL(new BigDecimal("100.00"));
        assertEquals(a, b);
    }

    @Test
    void equals_returnsFalse_whenCurrenciesDiffer() {
        Money brl = Money.ofBRL(new BigDecimal("100.00"));
        Money usd = Money.ofUSD(new BigDecimal("100.00"));
        assertNotEquals(brl, usd);
    }

    @Test
    void equals_returnsFalse_whenAmountsDiffer() {
        Money a = Money.ofBRL(new BigDecimal("100.00"));
        Money b = Money.ofBRL(new BigDecimal("200.00"));
        assertNotEquals(a, b);
    }

    @Test
    void hashCode_isEqual_forValuesEqualByCompareTo() {
        Money a = Money.ofBRL(new BigDecimal("100"));
        Money b = Money.ofBRL(new BigDecimal("100.00"));
        // Regression: previously violated equals/hashCode contract
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashCode_isDifferent_forDifferentAmounts() {
        Money a = Money.ofBRL(new BigDecimal("100.00"));
        Money b = Money.ofBRL(new BigDecimal("200.00"));
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    // ─── toString ────────────────────────────────────────────────────────────

    @Test
    void toString_containsCurrencySymbolAndAmount() {
        Money money = Money.ofBRL(new BigDecimal("99.90"));
        String result = money.toString();
        assertTrue(result.contains("R$"));
        assertTrue(result.contains("99.90"));
    }
}
