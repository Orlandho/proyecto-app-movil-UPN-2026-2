Feature: Registro de Usuarios (Backend)

  Scenario: AUT-001 Validar registro exitoso
    Given los datos de entrada son validos con nombre "Juan", email "ok@test.com", telefono "987654321", password "123" y rol "cliente"
    And el correo no existe previamente en la base de datos
    When se ejecuta el controlador de registro
    Then el sistema devuelve un codigo de estado HTTP 201
    And el JSON de respuesta contiene el mensaje "Usuario registrado exitosamente"

  Scenario: AUT-002 Bloqueo por campos ausentes
    Given los datos de entrada solo contienen el nombre "Juan"
    When se ejecuta el controlador de registro
    Then el sistema devuelve un codigo de estado HTTP 400
    And el JSON de respuesta contiene el error de campos obligatorios

  Scenario: AUT-002 Bloqueo por telefono invalido
    Given los datos de entrada son validos pero el telefono es "123"
    When se ejecuta el controlador de registro
    Then el sistema devuelve un codigo de estado HTTP 400
    And el JSON de respuesta contiene el error de telefono invalido

  Scenario: AUT-003 Restriccion de correo duplicado
    Given los datos de entrada son validos con nombre "Juan", email "duplicado@test.com", telefono "987654321", password "123"
    And el correo ya esta registrado en el sistema
    When se ejecuta el controlador de registro
    Then el sistema devuelve un codigo de estado HTTP 409
    And el JSON de respuesta contiene el error con codigo "EMAIL_ALREADY_EXISTS"

  Scenario: AUT-004 El registro publico no puede otorgar el rol de administrador
    Given los datos de entrada son validos con nombre "Intruso", email "intruso@test.com", telefono "987654321", password "123" y rol "admin"
    And el correo no existe previamente en la base de datos
    When se ejecuta el controlador de registro
    Then el sistema devuelve un codigo de estado HTTP 201
    And el usuario se crea con el rol "cliente"
    And la contrasena se guarda cifrada
