package org.houxg.leamonax.database;


import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.houxg.leamonax.model.Account;
import org.houxg.leamonax.model.Account_Table;

import java.util.List;

public class AccountDataStore {
    public static Account getAccount(String email, String host) {
        return SQLite.select()
                .from(Account.class)
                .where(Account_Table.email.eq(email))
                .and(Account_Table.host.eq(host))
                .querySingle();
    }

    public static Account getCurrent() {
        return SQLite.select()
                .from(Account.class)
                .where(Account_Table.token.notEq(""))
                .orderBy(Account_Table.lastUseTime, false)
                .querySingle();
    }

    public static List<Account> getAccountListWithToken() {
        return SQLite.select()
                .from(Account.class)
                .where(Account_Table.token.notEq(""))
                .orderBy(Account_Table.lastUseTime, false)
                .queryList();
    }

    public static Account getAccountById(long id) {
        return new Select()
                .from(Account.class)
                .where(Account_Table.id.eq(id))
                .querySingle();
    }
}
