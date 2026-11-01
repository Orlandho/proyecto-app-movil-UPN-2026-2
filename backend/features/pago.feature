Feature: CP-11 Reglas de cobro

  Scenario Outline: PAG-001 El estado del pago depende del metodo elegido
    Given el metodo de pago es "<metodo>"
    Then el estado del pago resultante es "<estado_pago>"
    And el estado inicial del pedido es "<estado_pedido>"

    Examples:
      | metodo | estado_pago | estado_pedido |
      | cash   | pendiente   | pendiente     |
      | card   | completado  | confirmado    |
      | wallet | completado  | confirmado    |

  Scenario Outline: PAG-002 Solo se aceptan los metodos soportados
    Given el metodo de pago es "<metodo>"
    Then el metodo se considera <valido>

    Examples:
      | metodo  | valido |
      | cash    | true   |
      | card    | true   |
      | wallet  | true   |
      | bitcoin | false  |
      |         | false  |

  Scenario Outline: PAG-003 El efectivo queda cobrado al entregar el pedido
    Given el metodo de pago es "<metodo>"
    And el pago esta "<estado_actual>"
    When el pedido pasa a "<estado_pedido>"
    Then el pago queda "<resultado>"

    Examples:
      | metodo | estado_actual | estado_pedido | resultado  |
      | cash   | pendiente     | entregado     | completado |
      | cash   | pendiente     | en_camino     | sin cambio |
      | cash   | pendiente     | cancelado     | sin cambio |
      | card   | completado    | entregado     | sin cambio |
      | wallet | completado    | entregado     | sin cambio |
