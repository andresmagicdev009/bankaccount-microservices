package com.application.service.it.support;

/**
 * Datos sembrados por schemas/BaseDatos.sql: son los casos de uso del enunciado.
 *
 * Estan aqui como constantes y no escritos a mano en cada test para que, si el
 * entregable cambia, se corrija en un solo sitio y la suite avise en vez de
 * fallar en veinte lugares con numeros magicos.
 */
public final class Seed {

    private Seed() {
    }

    // --------------------------------------------------------------- clientes

    public static final String JOSE_LEMA = "11111111-1111-1111-1111-111111111111";
    public static final String MARIANELA_MONTALVO = "22222222-2222-2222-2222-222222222222";
    public static final String JUAN_OSORIO = "33333333-3333-3333-3333-333333333333";

    public static final String JOSE_LEMA_NAME = "Jose Lema";
    public static final String JOSE_LEMA_ID = "1710034065";

    /** Cliente valido que NO tiene cuentas sembradas. */
    public static final String CUSTOMER_WITHOUT_ACCOUNTS = "44444444-4444-4444-4444-444444444444";

    // --------------------------------------------------------------- cuentas

    /** Jose Lema. SAVINGS, inicial 2000.00, disponible 1425.00, un DEBIT de 575. */
    public static final String ACCOUNT_JOSE_SAVINGS = "478758";

    /** Marianela. CHECKING, inicial 100.00, disponible 700.00, un CREDIT de 600. */
    public static final String ACCOUNT_MARIANELA_CHECKING = "225487";

    /** Juan Osorio. SAVINGS, inicial 0.00, disponible 150.00, un CREDIT de 150. */
    public static final String ACCOUNT_JUAN_SAVINGS = "495878";

    /** Marianela. SAVINGS, inicial 540.00, disponible 0.00: la unica borrable. */
    public static final String ACCOUNT_MARIANELA_EMPTY = "496825";

    /** Jose Lema. CHECKING, inicial 1000.00, disponible 1000.00, sin movimientos. */
    public static final String ACCOUNT_JOSE_CHECKING = "585545";

    /** Cuenta que no existe en el entregable. */
    public static final String UNKNOWN_ACCOUNT = "000000";

    // ------------------------------------------------------------ movimientos

    /** DEBIT 575.00 sobre 478758, fechado 2022-02-08. */
    public static final String MOVEMENT_JOSE_DEBIT = "aaaaaaaa-0000-0000-0000-000000000001";

    /** CREDIT 600.00 sobre 225487, fechado 2022-02-10. */
    public static final String MOVEMENT_MARIANELA_CREDIT = "aaaaaaaa-0000-0000-0000-000000000002";

    /** CREDIT 150.00 sobre 495878, fechado 2022-02-09. */
    public static final String MOVEMENT_JUAN_CREDIT = "aaaaaaaa-0000-0000-0000-000000000003";

    /** DEBIT 540.00 sobre 496825, fechado 2022-02-08. */
    public static final String MOVEMENT_MARIANELA_DEBIT = "aaaaaaaa-0000-0000-0000-000000000004";

    public static final String UNKNOWN_MOVEMENT = "aaaaaaaa-0000-0000-0000-999999999999";

    /** Todos los movimientos sembrados caen dentro de este rango. */
    public static final String SEEDED_RANGE_START = "2022-02-01";
    public static final String SEEDED_RANGE_END = "2022-02-28";
}
