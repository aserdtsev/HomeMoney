package ru.serdtsev.homemoney.moneyoper;

public enum MoneyOperStatus {
  /**
   * в ожидании
   */
  pending,
  /**
   * ожидает повтора
   */
  recurrence,
  /**
   * выполнен
   */
  done,
  /**
   * выполнен (новый)
   */
  doneNew,
  /**
   * отменен
   */
  cancelled
}
