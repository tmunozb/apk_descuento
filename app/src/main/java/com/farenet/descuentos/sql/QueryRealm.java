package com.farenet.descuentos.sql;

import com.farenet.descuentos.domain.Autorizadores;
import com.farenet.descuentos.domain.Conceptoinspeccion;
import com.farenet.descuentos.domain.Planta;
import com.farenet.descuentos.domain.TipoPagoDescuento;

import java.util.Collections;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public final class QueryRealm {

    private QueryRealm() {}

    // =========================
    // 1) API "WITH REALM" (managed)
    // =========================

    public static void insertOrUpdatePlantas(Realm realm, List<Planta> plantas) {
        if (plantas == null || plantas.isEmpty()) return;
        realm.insertOrUpdate(plantas);
    }

    public static void insertOrUpdateConceptos(Realm realm, List<Conceptoinspeccion> conceptos) {
        if (conceptos == null || conceptos.isEmpty()) return;
        realm.insertOrUpdate(conceptos);
    }

    public static void insertOrUpdateTipoPago(Realm realm, List<TipoPagoDescuento> tipos) {
        if (tipos == null || tipos.isEmpty()) return;
        realm.insertOrUpdate(tipos);
    }

    public static void insertOrUpdateAutorizadores(Realm realm, List<Autorizadores> autorizadores) {
        if (autorizadores == null || autorizadores.isEmpty()) return;
        realm.insertOrUpdate(autorizadores);
    }

    // =========================
    // 2) API "STANDALONE" (sin UI thread writes)
    //    -> MÉTODOS ASYNC con callbacks
    // =========================

    public interface TxCallback {
        void onSuccess();
        void onError(Throwable error);
    }

    public static void savePlantaAsync(final List<Planta> plantas, final TxCallback cb) {
        if (plantas == null || plantas.isEmpty()) { if (cb != null) cb.onSuccess(); return; }
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                r -> r.insertOrUpdate(plantas),
                () -> { if (cb != null) cb.onSuccess(); realm.close(); },
                e -> { if (cb != null) cb.onError(e); realm.close(); }
        );
    }

    public static void saveConceptosAsync(final List<Conceptoinspeccion> conceptos, final TxCallback cb) {
        if (conceptos == null || conceptos.isEmpty()) { if (cb != null) cb.onSuccess(); return; }
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                r -> r.insertOrUpdate(conceptos),
                () -> { if (cb != null) cb.onSuccess(); realm.close(); },
                e -> { if (cb != null) cb.onError(e); realm.close(); }
        );
    }

    public static void saveTipoPagoAsync(final List<TipoPagoDescuento> tipos, final TxCallback cb) {
        if (tipos == null || tipos.isEmpty()) { if (cb != null) cb.onSuccess(); return; }
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                r -> r.insertOrUpdate(tipos),
                () -> { if (cb != null) cb.onSuccess(); realm.close(); },
                e -> { if (cb != null) cb.onError(e); realm.close(); }
        );
    }

    public static void saveAutorizadoresAsync(final List<Autorizadores> autorizadores, final TxCallback cb) {
        if (autorizadores == null || autorizadores.isEmpty()) { if (cb != null) cb.onSuccess(); return; }
        final Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(
                r -> r.insertOrUpdate(autorizadores),
                () -> { if (cb != null) cb.onSuccess(); realm.close(); },
                e -> { if (cb != null) cb.onError(e); realm.close(); }
        );
    }

    // =========================
    // 3) GETTERS
    // =========================

    // Managed (caller mantiene Realm)
    public static RealmResults<Planta> getAllPlantas(Realm realm) {
        return realm.where(Planta.class).findAll().sort("nombre", Sort.ASCENDING);
    }

    public static RealmResults<Conceptoinspeccion> getAllConceptos(Realm realm) {
        return realm.where(Conceptoinspeccion.class).findAll().sort("abreviatura", Sort.ASCENDING);
    }

    public static RealmResults<TipoPagoDescuento> getAllTipoPagos(Realm realm) {
        return realm.where(TipoPagoDescuento.class).findAll().sort("nombre", Sort.ASCENDING);
    }

    public static RealmResults<Autorizadores> getAllAutorizadores(Realm realm) {
        return realm.where(Autorizadores.class).findAll().sort("nombre", Sort.ASCENDING);
    }

    // Unmanaged (copias)
    public static List<Planta> copyAllPlantas() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Planta> res = realm.where(Planta.class)
                    .findAll()
                    .sort("nombre", Sort.ASCENDING);
            return realm.copyFromRealm(res);
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public static List<Conceptoinspeccion> copyAllConceptos() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Conceptoinspeccion> res = realm.where(Conceptoinspeccion.class)
                    .findAll()
                    .sort("abreviatura", Sort.ASCENDING);
            return realm.copyFromRealm(res);
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public static List<TipoPagoDescuento> copyAllTipoPagos() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<TipoPagoDescuento> res = realm.where(TipoPagoDescuento.class)
                    .findAll()
                    .sort("nombre", Sort.ASCENDING);
            return realm.copyFromRealm(res);
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }

    public static List<Autorizadores> copyAllAutorizadores() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Autorizadores> res = realm.where(Autorizadores.class)
                    .findAll()
                    .sort("nombre", Sort.ASCENDING);
            return realm.copyFromRealm(res);
        } catch (Throwable t) {
            return Collections.emptyList();
        }
    }
}
