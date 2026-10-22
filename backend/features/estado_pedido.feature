Feature: CP-10 Maquina de estados del pedido

  Scenario Outline: EST-001 Solo se admite avanzar un paso en el flujo
    Given un pedido en estado "<actual>"
    When se intenta cambiar el estado a "<siguiente>"
    Then la transicion es <permitida>

    Examples:
      | actual         | siguiente      | permitida |
      | pendiente      | confirmado     | true      |
      | confirmado     | en_preparacion | true      |
      | en_preparacion | en_camino      | true      |
      | en_camino      | entregado      | true      |
      | pendiente      | entregado      | false     |
      | entregado      | en_camino      | false     |
      | cancelado      | confirmado     | false     |

  Scenario Outline: EST-002 La cancelacion solo se permite antes de entrar a cocina
    Given un pedido en estado "<actual>"
    When se intenta cambiar el estado a "cancelado"
    Then la transicion es <permitida>

    Examples:
      | actual         | permitida |
      | pendiente      | true      |
      | confirmado     | true      |
      | en_preparacion | false     |
      | en_camino      | false     |

  Scenario: EST-003 Los estados heredados de la base se normalizan antes de comparar
    Given un pedido en estado "En preparación"
    When se intenta cambiar el estado a "en_camino"
    Then la transicion es true
    And el estado normalizado del pedido es "en_preparacion"

  Scenario: EST-004 El siguiente estado de un pedido entregado no existe
    Given un pedido en estado "entregado"
    Then el pedido se considera terminal
    And no hay siguiente estado
