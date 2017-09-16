package ru.serdtsev.homemoney.account;

public enum AccountType {
  debit, credit, expense, income, reserve, asset, service;
  public boolean isBalance() {
    return this.equals(debit) || this.equals(credit);
  }
}