Feature: CP-15 Simulación del pedido y del reparto

  Cada pedido nuevo nace con un plan: cuántos segundos dura cada estado. El
  simulador lo avanza cuando vence y el seguimiento lo usa para estimar horas.

  Background:
    Given la simulación con escala 10, confirmación 2 min, preparación 12 min, reparto máximo 40 min y 25 km/h

  Scenario: UT-SIM-001 El plan reparte los tiempos configurados
    When se crea el plan de un pedido nuevo
    Then el estado "pendiente" dura 12 segundos
    And el estado "confirmado" dura 6 segundos
    And el estado "en_preparacion" dura 66 segundos
    And el reparto aún no tiene duración

  Scenario: UT-SIM-002 Con la simulación apagada no hay plan
    Given la simulación está apagada
    When se crea el plan de un pedido nuevo
    Then no hay plan

  Scenario Outline: UT-SIM-003 El reparto dura según la distancia, con tope
    When se calcula la duración de un reparto de <metros> metros
    Then el reparto dura <segundos> segundos

    Examples:
      | metros | segundos |
      | 6000   | 86       |
      | 200    | 6        |
      | 300000 | 240      |

  Scenario Outline: UT-SIM-004 Un estado vence al cumplirse su tiempo
    Given un pedido en "en_preparacion" desde hace <transcurridos> segundos con un plan de 66 segundos para ese estado
    Then el estado está vencido: <vencido>

    Examples:
      | transcurridos | vencido |
      | 30            | false   |
      | 66            | true    |
      | 120           | true    |

  Scenario: UT-SIM-005 Un estado sin duración en el plan nunca vence
    Given un pedido en "en_camino" desde hace 9999 segundos sin duración de reparto en el plan
    Then el estado está vencido: false

  Scenario: UT-SIM-006 El repartidor va por la mitad a mitad del tiempo
    Given un pedido en camino por una ruta recta de 1000 metros que dura 100 segundos
    When han pasado 50 segundos desde que salió
    Then el repartidor lleva el 50 por ciento del recorrido
    And le faltan 500 metros

  Scenario: UT-SIM-007 Un pedido entregado deja al repartidor en la puerta
    Given un pedido en camino por una ruta recta de 1000 metros que dura 100 segundos
    And el pedido ya figura como entregado
    When han pasado 10 segundos desde que salió
    Then el repartidor lleva el 100 por ciento del recorrido

  Scenario: UT-SIM-008 Una ruta larga se aligera sin perder sus extremos
    Given una ruta de 5000 puntos
    When se simplifica a 300 puntos como máximo
    Then quedan como mucho 300 puntos
    And conserva el primer y el último punto
