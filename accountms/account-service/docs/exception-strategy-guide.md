# Guia: Strategy para excepciones — version minima

Empezamos con lo mas simple que cumple SRP y OCP. Al final esta la escalera para
crecerlo por niveles cuando lo necesites.

---

## Nivel 1 — lo minimo

Cuatro cosas: la jerarquia de dominio, una interfaz de 2 metodos, 5 strategies
diminutas y el advice.

### 1. Jerarquia de dominio (ya tienes `DomainException`)

El contrato emite 400, 404, 409, 422, 500 y 502. El 500 no lo lanzas: es lo que
queda cuando nada mas aplica. Quedan **5 categorias**.

```
DomainException (abstract, ya creada)
├── EntradaInvalidaException          -> 400
├── RecursoNoEncontradoException      -> 404
├── ConflictoEstadoException          -> 409
├── ReglaNegocioVioladaException      -> 422
└── DependenciaExternaException       -> 502
```

Las 5 en `domain/shared/exception/`, todas `abstract`: nadie lanza la categoria,
se lanza la concreta.

```java
public abstract class RecursoNoEncontradoException extends DomainException {
    protected RecursoNoEncontradoException(String code, String message) {
        super(code, message);
    }
}
```

Las concretas que ya tienes solo cambian de padre:

```java
public class AccountNotFoundException extends RecursoNoEncontradoException {
    public AccountNotFoundException(String accountNumber) {
        super("ACCOUNT_NOT_FOUND", "Account not found with number: " + accountNumber);
    }
}
```

Clasificacion de las 9:

| Concreta | Categoria | Status |
|---|---|---|
| `AccountNotFoundException` | `RecursoNoEncontradoException` | 404 |
| `MovementNotFoundException` | `RecursoNoEncontradoException` | 404 |
| `CustomerNotFoundException` | `RecursoNoEncontradoException` | 404 |
| `InvalidMovementValueException` | `EntradaInvalidaException` | 400 |
| `InvalidPageSizeException` | `EntradaInvalidaException` | 400 |
| `InvalidDateRangeException` | `EntradaInvalidaException` | 400 |
| `AccountBalanceNotZeroException` | `ConflictoEstadoException` | 409 |
| `InsufficientBalanceException` | `ReglaNegocioVioladaException` | 422 |
| `CustomerServiceUnavailableException` | `DependenciaExternaException` | 502 |

> 400 vs 422: el YAML manda. `POST /movements` declara `'400': value is not
> greater than zero`, asi que valor <= 0 es 400 aunque suene a regla de negocio.

Regla que no se rompe: **nada de `HttpStatus` en `domain/`**. El dominio no
conoce HTTP.

### 2. La interfaz — 2 metodos, cero logica

`interfaces/rest/advice/strategy/ExceptionStrategy.java`

```java
public interface ExceptionStrategy {

    /** Categoria de dominio que cubre esta strategy. */
    Class<? extends DomainException> handles();

    /** Status del contrato para esa categoria. */
    HttpStatus status();
}
```

Aqui vive el unico conocimiento que se repite en todo el manejo de errores:
**que categoria de dominio corresponde a que status HTTP**. Nada mas.

### 3. Las 5 strategies — 8 lineas cada una

`interfaces/rest/advice/strategy/impl/NotFoundStrategy.java`

```java
@Component
public class NotFoundStrategy implements ExceptionStrategy {

    @Override
    public Class<? extends DomainException> handles() {
        return RecursoNoEncontradoException.class;
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
```

Las otras cuatro son identicas cambiando las dos lineas:

| Clase | `handles()` | `status()` |
|---|---|---|
| `BadRequestStrategy` | `EntradaInvalidaException` | `BAD_REQUEST` |
| `NotFoundStrategy` | `RecursoNoEncontradoException` | `NOT_FOUND` |
| `ConflictStrategy` | `ConflictoEstadoException` | `CONFLICT` |
| `UnprocessableStrategy` | `ReglaNegocioVioladaException` | `UNPROCESSABLE_CONTENT` |
| `BadGatewayStrategy` | `DependenciaExternaException` | `BAD_GATEWAY` |

> Spring 7: `UNPROCESSABLE_CONTENT`. `UNPROCESSABLE_ENTITY` quedo deprecada.

### 4. El advice

```java
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    /** Spring inyecta TODAS las implementaciones. Aqui esta el OCP. */
    private final List<ExceptionStrategy> strategies;

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorDto> handleDomain(DomainException ex, ServerWebExchange exchange) {
        HttpStatus status = strategies.stream()
                .filter(strategy -> strategy.handles().isInstance(ex))
                .findFirst()
                .map(ExceptionStrategy::status)
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);

        log.warn("{} [{}] -> {}", ex.getClass().getSimpleName(), ex.getCode(), status.value());
        return ResponseEntity.status(status).body(build(status, ex.getMessage(), exchange));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception ex, ServerWebExchange exchange) {
        log.error("Error no controlado", ex);   // stacktrace SOLO aqui
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status)
                .body(build(status, "Error interno del servidor", exchange));  // mensaje generico
    }

    private ErrorDto build(HttpStatus status, String message, ServerWebExchange exchange) {
        // el que ya tienes escrito
    }
}
```

`isInstance` (no `equals`) es la pieza clave: cubre todas las subclases de la
categoria sin registrarlas.

**Eso es todo el Nivel 1.** ~40 lineas de codigo nuevo.

### Por que ya cumple

| Principio | Como |
|---|---|
| **SRP** | Cada strategy sabe una cosa: categoria -> status. El advice sabe otra: armar la respuesta HTTP. El dominio no sabe ninguna de las dos. |
| **OCP** | Excepcion nueva que extiende una categoria: **cero** archivos editados. Status nuevo en el contrato: un `@Component` nuevo, tambien cero ediciones. |

### Orden para escribirlo

1. Las 5 categorias abstractas.
2. Reapuntar las 9 concretas a su categoria (el compilador te guia).
3. `ExceptionStrategy`.
4. Las 5 strategies.
5. El advice.
6. Un test por endpoint del YAML que devuelva error.

---

## La escalera — que agregar y cuando

Cada nivel es opcional e independiente. Sube solo cuando el problema aparezca.

### Nivel 2 — mensaje propio por categoria
**Sintoma:** el 422 debe decir exactamente `"Saldo no disponible"`, o quieres
ocultar el mensaje interno de la excepcion externa en el 502.

Agrega un tercer metodo con `default` a la interfaz — no rompe las strategies
que ya escribiste:

```java
default String messageFor(DomainException ex) {
    return ex.getMessage();
}
```

y sobreescribelo solo donde haga falta.

### Nivel 3 — excepciones del framework
**Sintoma:** `@Valid` falla y devuelve el JSON de error de Spring, no tu
`ErrorDto`.

Un `@ExceptionHandler` mas en el advice para `WebExchangeBindException` y
`ServerWebInputException` -> 400. No pasan por strategies: no son de tu dominio.

### Nivel 4 — extraer `ErrorResponseFactory`
**Sintoma:** un segundo sitio necesita armar `ErrorDto` (un `WebFilter`, un
`ErrorWebExceptionHandler` para errores fuera del advice).

Saca el `build(...)` privado a un `@Component` con un metodo publico.

### Nivel 5 — extraer `ExceptionStrategyResolver`
**Sintoma:** quieres testear la seleccion de strategy sin levantar el advice, o
la busqueda lineal ya no basta y quieres cachear un `Map<Class, ExceptionStrategy>`.

Saca el `stream()...findFirst()` a un `@Component` con `resolve(Throwable)`.

### Nivel 6 — la strategy construye la respuesta completa
**Sintoma:** una categoria necesita algo que las otras no: cabecera
`Retry-After` en el 502, nivel de log distinto, campos extra.

Cambia la interfaz a `ResponseEntity<ErrorDto> handle(DomainException, ServerWebExchange)`
y mete un `AbstractStatusStrategy` (Template Method) que implemente el caso comun
a partir de `handles()` + `status()`, para que las 5 existentes no cambien.

### Nivel 7 — jerarquias solapadas
**Sintoma:** dos strategies soportan la misma excepcion (te pasara si algun dia
manejas `ResponseStatusException`, del que `ServerWebInputException` hereda).

`@Order` en cada strategy — Spring inyecta la `List` ya ordenada y gana el primer
`findFirst()`. Lo especifico primero.

### Nivel 8 — `code` en la respuesta
**Sintoma:** el front quiere distinguir errores sin parsear el `message`.

Contract first: agregas `code` al schema `Error` del YAML, `./mvnw
generate-sources`, y recien ahi el advice lo escribe desde `ex.getCode()`.
Hasta entonces `code` es solo para logs y tests.
