package com.farenet.descuentos.sql;

import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.Sort;

public class QueryRealm {

    public static void savePlanta(final List<Planta> plantas) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmList<Planta> realmList = new RealmList<>();
                realmList.addAll(plantas);
                realm.insertOrUpdate(realmList);
            }
        });
    }

    public static List<Planta> getAllPlantas() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Planta.class).findAll().sort("nombre", Sort.ASCENDING);
    }

    public static void saveConceptos(final List<Conceptoinspeccion> conceptoinspeccions) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmList<Conceptoinspeccion> realmList = new RealmList<>();
                realmList.addAll(conceptoinspeccions);
                realm.insertOrUpdate(realmList);
            }
        });
    }

    public static List<Conceptoinspeccion> getAllConcepto() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(Conceptoinspeccion.class).findAll().sort("abreviatura", Sort.ASCENDING);
    }

    public static void saveTipoPago(final List<TipoPagoDescuento> tipoPagoDescuentos) {
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmList<TipoPagoDescuento> realmList = new RealmList<>();
                realmList.addAll(tipoPagoDescuentos);
                realm.insertOrUpdate(realmList);
            }
        });
    }

    public static List<TipoPagoDescuento> getAllTipoPagos() {
        Realm realm = Realm.getDefaultInstance();
        return realm.where(TipoPagoDescuento.class).findAll().sort("nombre", Sort.ASCENDING);
    }
}
