package com.carrefourbank.common.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Classe de valor imutável que representa um valor monetário com uma moeda associada.
 */
public class Money {
    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }

    /**
     * Cria uma instância de Money com o valor e moeda especificados.
     *
     * @param amount O valor monetário
     * @param currency A moeda
     * @return Uma nova instância de Money
     */
    public static Money of(BigDecimal amount, Currency currency) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        return new Money(amount, currency);
    }

    /**
     * Cria uma instância de Money com o valor e moeda especificados.
     *
     * @param amount O valor monetário
     * @param currencyCode O código da moeda como string
     * @return Uma nova instância de Money
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");
        Currency currency = Currency.valueOf(currencyCode);
        return of(amount, currency);
    }

    /**
     * Cria uma instância de Money em Reais (BRL).
     *
     * @param amount O valor monetário
     * @return Uma nova instância de Money em BRL
     */
    public static Money ofBRL(BigDecimal amount) {
        return of(amount, Currency.BRL);
    }

    /**
     * Cria uma instância de Money em Dólares (USD).
     *
     * @param amount O valor monetário
     * @return Uma nova instância de Money em USD
     */
    public static Money ofUSD(BigDecimal amount) {
        return of(amount, Currency.USD);
    }

    /**
     * Cria uma instância de Money com valor zero na moeda especificada.
     *
     * @param currency A moeda
     * @return Uma nova instância de Money com valor zero
     */
    public static Money zero(Currency currency) {
        return of(BigDecimal.ZERO, currency);
    }

    /**
     * Verifica se o valor é zero.
     *
     * @return true se o valor for zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Verifica se o valor é positivo.
     *
     * @return true se o valor for maior que zero
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Verifica se o valor é negativo.
     *
     * @return true se o valor for menor que zero
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Adiciona outro valor monetário a este.
     * As moedas devem ser compatíveis.
     *
     * @param other O outro valor monetário a adicionar
     * @return Um novo Money representando a soma
     * @throws IllegalArgumentException se as moedas forem diferentes
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot add money with different currencies: " + this.currency + " and " + other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtrai outro valor monetário deste.
     * As moedas devem ser compatíveis.
     *
     * @param other O outro valor monetário a subtrair
     * @return Um novo Money representando a diferença
     * @throws IllegalArgumentException se as moedas forem diferentes
     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot subtract money with different currencies: " + this.currency + " and " + other.currency);
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Multiplica este valor monetário por um fator.
     *
     * @param factor O fator de multiplicação
     * @return Um novo Money representando o produto
     */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Factor cannot be null");
        return new Money(this.amount.multiply(factor), this.currency);
    }

    /**
     * Divide este valor monetário por um divisor.
     *
     * @param divisor O divisor
     * @return Um novo Money representando o quociente
     * @throws ArithmeticException se o divisor for zero
     */
    public Money divide(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "Divisor cannot be null");
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return new Money(this.amount.divide(divisor), this.currency);
    }

    /**
     * Nega o valor monetário, invertendo seu sinal.
     *
     * @return Um novo Money com o valor negado
     */
    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    /**
     * Retorna o valor absoluto do valor monetário.
     *
     * @return Um novo Money com o valor absoluto
     */
    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    /**
     * Obtém o valor monetário.
     *
     * @return O valor como BigDecimal
     */
    public BigDecimal amount() {
        return amount;
    }

    /**
     * Obtém a moeda.
     *
     * @return A moeda
     */
    public Currency currency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency == money.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return currency.getSymbol() + " " + amount;
    }
}