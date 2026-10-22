Feature: CP-14 Coordenadas de locales y direcciones de entrega

  Las dos puntas de la ruta del reparto. El backend no se fía del navegador:
  una coordenada inválida dibujaría al repartidor yendo a cualquier parte.

  Scenario Outline: UT-COORD-001 Validación del par latitud/longitud
    When se validan las coordenadas "<lat>" y "<lng>"
    Then las coordenadas se aceptan: <aceptadas>
    And el error es "<error>"

    Examples:
      | lat    | lng    | aceptadas | error                                             |
      | -12.05 | -77.04 | true      |                                                   |
      |        |        | true      |                                                   |
      | -12.05 |        | false     | Indica latitud y longitud, o ninguna de las dos.  |
      |        | -77.04 | false     | Indica latitud y longitud, o ninguna de las dos.  |
      | 120    | -77.04 | false     | La latitud debe estar entre -90 y 90.             |
      | -12.05 | 200    | false     | La longitud debe estar entre -180 y 180.          |
      | abc    | -77.04 | false     | La latitud debe estar entre -90 y 90.             |
