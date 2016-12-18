package su.gear.walletpad.model;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Объект кошелька
 */

public class Wallet implements WalletsListItem {

    public enum Type {
        CARD,
        CASH,
        DEPOSIT,
        STOCK,
        OTHER;

        public static Type fetchType (String type) {
            if (type != null && type.length () > 0) {
                type = type.toLowerCase ().trim ();

                if (type.equals ("card"))    { return CARD; }
                if (type.equals ("cash"))    { return CASH; }
                if (type.equals ("deposit")) { return DEPOSIT; }
                if (type.equals ("stock"))   { return STOCK; }
            }

            return OTHER;
        }
    }

    /**
     * ID кошелька
     */
    private final long id;

    /**
     * Тип кошелька
     */
    private final Type type;

    /**
     * Валюта операции
     */
    private final String currency;

    /**
     * Сумма
     */
    private final double amount;

    /**
     * Название кошелька
     */
    private final String title;

    /**
     * Флаг отображения суммы с кошелька в общей сумме
     */
    private final boolean showInTotal;

    /**
     * История операций кошелька
     */
    private List<Operation> operations;

    public Wallet(double amount, long id, Type type, String currency, String title, boolean showInTotal, List<Operation> operations) {
        this.amount = amount;
        this.id = id;
        this.type = type;
        this.currency = currency;
        this.title = title;
        this.showInTotal = showInTotal;
        this.operations = operations;
    }

    public double getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public long getId() {
        return id;
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public boolean isShowInTotal() {
        return showInTotal;
    }

    public String getTitle() {
        return title;
    }

    public Type getType() {
        return type;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        // TODO: Implement
        return map;
    }
}
