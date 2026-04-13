package com.carrefourbank.common.domain;

/**
 * Enum que representa as moedas suportadas pelo sistema.
 */
public enum Currency {
    BRL("R$"),  // Real Brasileiro
    USD("$"),   // Dólar Americano
    EUR("€"),   // Euro
    GBP("£");   // Libra Esterlina

    private final String symbol;

    Currency(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Obtém o símbolo da moeda.
     *
     * @return O símbolo da moeda
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Verifica se a moeda é a moeda padrão (BRL).
     *
     * @return true se for BRL, false caso contrário
     */
    public boolean isDefault() {
        return this == BRL;
    }
}