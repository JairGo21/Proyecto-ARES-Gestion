## Qué hace este PR

<!-- Una o dos frases. Ejemplo: Implementa el escenario principal de CU-07 y las extensiones de nombre vacío y duplicado. -->

Closes #

## Cómo probarlo

1. `./mvnw spring-boot:run`
2. Ir a ...
3. Debería verse ...

## Lista de revisión

- [ ] `./mvnw verify` pasa en mi máquina después de traer lo último de `main`.
- [ ] El Controller solo llama al Service; ningún Controller usa un Repository.
- [ ] Las reglas del negocio están en las entidades; el Service solo busca, pide y guarda.
- [ ] El método del Service que guarda tiene `@Transactional`.
- [ ] Los valores no numéricos se rechazan en la vista o el Controller; los de 0 o negativos, en la entidad.
- [ ] Hay pruebas unitarias para cada regla nueva (caso válido y casos rechazados).
- [ ] No edité una migración que ya estaba en `main` (los cambios de esquema van en un `V#__` nuevo).
- [ ] Los nombres de clases y métodos coinciden con los diagramas (o dejé anotado qué diagrama hay que actualizar).
