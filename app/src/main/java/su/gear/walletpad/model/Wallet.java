package su.gear.walletpad.model;

import com.google.firebase.database.DataSnapshot;

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

        public static Type fetchType (int index) {
            if (index >= 0 && index < values ().length) {
                return values () [index];
            }

            return null;
        }
    }

    /**
     * ID кошелька
     */
    private final String id;

    /**
     * Тип кошелька
     */
    private final Type type;

    /**
     * Валюта операции
     */
    private final Currency currency;

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

    public Wallet (DataSnapshot snapshot) {
        this.id = "0";
        this.amount      = Double.parseDouble ((String) snapshot.child ("amount").getValue ());
        this.type        = Type.fetchType ((String) snapshot.child ("type").getValue ());
        this.currency    = Currency.getInstance ((String) snapshot.child ("currency").getValue ());
        this.title       = (String) snapshot.child ("title").getValue ();
        this.showInTotal = Boolean.parseBoolean ((String) snapshot.child ("intotal").getValue ());
        this.operations  = null;
    }

    public Wallet(double amount, String id, Type type, Currency currency, String title, boolean showInTotal, List<Operation> operations) {
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

    public Currency getCurrency() {
        return currency;
    }

    public String getId() {
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

    public Map <String, Object> toMap () {
        Map <String, Object> result = new HashMap <> ();
        result.put ("amount",      amount + "");
        result.put ("type",        type);
        result.put ("currency",    currency.toString ());
        result.put ("title",       title);
        result.put ("intotal",     showInTotal + "");

        return result;
    }
}
