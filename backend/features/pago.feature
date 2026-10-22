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
