package com.carrefourbank.common.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyTest {

    @Test
    void brl_isDefaultCurrency() {
        assertTrue(Currency.BRL.isDefault());
    }

    @Test
    void nonBrl_isNotDefaultCurrency() {
        assertFalse(Currency.USD.isDefault());
        assertFalse(Currency.EUR.isDefault());
        assertFalse(Currency.GBP.isDefault());
    }

    @Test
    void getSymbol_returnsCorrectSymbolForEachCurrency() {
        assertEquals("R$", Currency.BRL.getSymbol());
        assertEquals("$",  Currency.USD.getSymbol());
        assertEquals("€",  Currency.EUR.getSymbol());
        assertEquals("£",  Currency.GBP.getSymbol());
    }

    @Test
    void valueOf_returnsCorrectConstant() {
        assertEquals(Currency.BRL, Currency.valueOf("BRL"));
        assertEquals(Currency.USD, Currency.valueOf("USD"));
    }
}
