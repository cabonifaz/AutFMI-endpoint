USE [bdt_staging];
SET XACT_ABORT ON;
IF DB_NAME()<>N'bdt_staging' THROW 52240,'Solo bdt_staging',1;
IF @@TRANCOUNT<>0 THROW 52240,'Ejecutar sin transaccion previa',1;
IF REPLACE(OBJECT_DEFINITION(OBJECT_ID('dbo.SP_FMI_SOLICITUD_APROBACION_REG')),CHAR(13),'')<>REPLACE(N'CREATE PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_REG
(
    @TIPO_SOLICITUD   VARCHAR(20),
    @ID_CLIENTE       INT,
    @ID_TALENTO       INT           = NULL,
    @NOMBRE_TALENTO   NVARCHAR(200) = NULL,
    @RESUMEN          NVARCHAR(300) = NULL,
    @DATOS_FORMULARIO NVARCHAR(MAX),
    -- IDENTIDAD DEL SOLICITANTE (del token validado por el backend)
    @ID_USUARIO         INT,
    @ID_EMPRESA         INT,
    @ID_ROLES           VARCHAR(100),
    -- Funcionalidades que exige la operación, como en el registro directo ("3,5").
    @ID_FUNCIONALIDADES VARCHAR(100),
    @USUARIO            VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Registra una solicitud PENDIENTE con su primer evento del
    --              historial y devuelve los gestores activos del cliente. No
    --              ejecuta la operación ni envía correos. El solicitante debe
    --              tener, en uno de sus roles vigentes del token, todas las
    --              funcionalidades de la operación (ROL_FUNCIONALIDAD).
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    SET @TIPO_SOLICITUD = UPPER(LTRIM(RTRIM(@TIPO_SOLICITUD)));
    DECLARE @ID_SOLICITUD INT, @ID_PENDIENTE INT;
    DECLARE @NUEVA TABLE (ID_SOLICITUD INT NOT NULL);
    DECLARE @ROLES VARCHAR(102) = '','' + REPLACE(ISNULL(@ID_ROLES, ''''), '' '', '''') + '','';
    DECLARE @FUNCS VARCHAR(100) = REPLACE(ISNULL(@ID_FUNCIONALIDADES, ''''), '' '', '''');

    BEGIN TRY
        IF @FUNCS = '''' OR NOT EXISTS (
            SELECT 1
            FROM dbo.USUARIO_ROL UR
            WHERE UR.ID_USUARIO = @ID_USUARIO
              AND UR.ID_ESTADO_REGISTRO = 1
              AND @ROLES LIKE ''%,'' + CONVERT(VARCHAR(11), UR.ID_TIPO_ROL) + '',%''
              AND (SELECT COUNT(DISTINCT RFN.ID_FUNCIONALIDAD)
                   FROM dbo.ROL_FUNCIONALIDAD RFN
                   WHERE RFN.ID_TIPO_ROL = UR.ID_TIPO_ROL
                     AND RFN.ID_EMPRESA = @ID_EMPRESA
                     AND RFN.ID_ESTADO_REGISTRO = 1
                     AND '','' + @FUNCS + '','' LIKE ''%,'' + CONVERT(VARCHAR(11), RFN.ID_FUNCIONALIDAD) + '',%'')
                  = LEN(@FUNCS) - LEN(REPLACE(@FUNCS, '','', '''')) + 1)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para registrar esta operación.'' AS MENSAJE,
                   CAST(NULL AS INT) AS ID_SOLICITUD;
            RETURN;
        END;

        IF @TIPO_SOLICITUD IS NULL OR @TIPO_SOLICITUD NOT IN (''INGRESO'', ''MOVIMIENTO'', ''CESE'')
           OR @DATOS_FORMULARIO IS NULL OR ISJSON(@DATOS_FORMULARIO) <> 1
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''Tipo de solicitud o datos del formulario no válidos.'' AS MENSAJE,
                   CAST(NULL AS INT) AS ID_SOLICITUD;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.USUARIOS
                       WHERE ID_USUARIO = @ID_USUARIO AND ID_EMPRESA = @ID_EMPRESA
                         AND ID_ESTADO_REGISTRO = 1)
           OR NOT EXISTS (SELECT 1 FROM dbo.CLIENTE
                          WHERE ID_CLIENTE = @ID_CLIENTE AND ID_EMPRESA = @ID_EMPRESA
                            AND ID_ESTADO_REGISTRO = 1)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''El usuario o el cliente no están activos en la empresa.'' AS MENSAJE,
                   CAST(NULL AS INT) AS ID_SOLICITUD;
            RETURN;
        END;

        BEGIN TRANSACTION;

        -- Un mismo talento no puede tener dos solicitudes pendientes del mismo
        -- tipo en el mismo cliente (doble clic, reenvío del formulario).
        IF @ID_TALENTO IS NOT NULL
            SELECT TOP (1) @ID_PENDIENTE = S.ID_SOLICITUD
            FROM dbo.FMI_SOLICITUD_APROBACION S WITH (UPDLOCK, HOLDLOCK)
            WHERE S.ID_TALENTO = @ID_TALENTO
              AND S.ID_CLIENTE = @ID_CLIENTE
              AND S.TIPO_SOLICITUD = @TIPO_SOLICITUD
              AND S.ESTADO = ''PENDIENTE''
              AND S.ID_ESTADO_REGISTRO = 1;

        IF @ID_PENDIENTE IS NOT NULL
        BEGIN
            COMMIT TRANSACTION;
            SELECT 1 AS ID_TIPO_MENSAJE, ''CONFLICTO'' AS CODIGO_RESULTADO,
                   ''Ya existe la solicitud pendiente N° '' + CAST(@ID_PENDIENTE AS VARCHAR(11))
                   + '' para este talento.'' AS MENSAJE,
                   @ID_PENDIENTE AS ID_SOLICITUD;
            RETURN;
        END;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION
            (ORIGEN_FLUJO, TIPO_SOLICITUD, ESTADO, ID_CLIENTE, ID_USUARIO_SOLICITANTE, ID_TALENTO,
             NOMBRE_TALENTO, RESUMEN, DATOS_FORMULARIO, USUCRE, FCHCRE, ID_ESTADO_REGISTRO)
        OUTPUT INSERTED.ID_SOLICITUD INTO @NUEVA (ID_SOLICITUD)
        VALUES
            (''FTBO-1124'', @TIPO_SOLICITUD, ''PENDIENTE'', @ID_CLIENTE, @ID_USUARIO, @ID_TALENTO,
             @NOMBRE_TALENTO, @RESUMEN, @DATOS_FORMULARIO, @USUARIO, GETDATE(), 1);

        SELECT @ID_SOLICITUD = ID_SOLICITUD FROM @NUEVA;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
            (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
        VALUES
            (@ID_SOLICITUD, ''CREADA'', NULL, ''PENDIENTE'', @RESUMEN, @ID_USUARIO, @USUARIO, GETDATE());

        COMMIT TRANSACTION;

        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Solicitud registrada correctamente.'' AS MENSAJE,
               @ID_SOLICITUD AS ID_SOLICITUD,
               (SELECT MIN(RAZON_SOCIAL) FROM dbo.CLIENTE
                WHERE ID_CLIENTE = @ID_CLIENTE AND ID_EMPRESA = @ID_EMPRESA) AS CLIENTE;

        -- GESTORES ACTIVOS DEL CLIENTE
        SELECT U.ID_USUARIO, U.USUARIO, MIN(UR.ID_TIPO_ROL) AS ID_TIPO_ROL
        FROM dbo.CLIENTE_GESTOR CG
        INNER JOIN dbo.USUARIOS U
            ON U.ID_USUARIO = CG.ID_USUARIO
           AND U.ID_EMPRESA = @ID_EMPRESA
           AND U.ID_ESTADO_REGISTRO = 1
        INNER JOIN dbo.USUARIO_ROL UR
            ON UR.ID_USUARIO = U.ID_USUARIO
           AND UR.ID_ESTADO_REGISTRO = 1
        WHERE CG.ID_CLIENTE = @ID_CLIENTE
          AND CG.ID_ESTADO_REGISTRO = 1
        GROUP BY U.ID_USUARIO, U.USUARIO;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               CAST(NULL AS INT) AS ID_SOLICITUD;
    END CATCH;
END;
-- FTBO1124-alcance-v2',CHAR(13),'') THROW 52240,'Cambio concurrente en SP_FMI_SOLICITUD_APROBACION_REG; revisar antes de instalar',1;
IF REPLACE(OBJECT_DEFINITION(OBJECT_ID('dbo.SP_FMI_SOLICITUD_APROBACION_LST')),CHAR(13),'')<>REPLACE(N'CREATE PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_LST
(
    -- FILTROS
    @ID_CLIENTE     INT          = NULL,
    @TIPO_SOLICITUD VARCHAR(20)  = NULL,
    @ESTADO         VARCHAR(20)  = NULL,
    -- Número exacto de solicitud, o texto contenido en el nombre del talento
    -- o en el resumen. Se usa solo como valor (LIKE con comodines escapados).
    @BUSQUEDA       NVARCHAR(100) = NULL,
    -- PAGINACION
    @NUM_PAG        INT          = 1,
    @CANT_PAG       INT          = 10,
    -- IDENTIDAD (del token validado por el backend)
    @ID_USUARIO     INT,
    @ID_EMPRESA     INT,
    @ID_ROLES       VARCHAR(100),
    @USUARIO        VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1118
    -- Descripción: Bandeja de aprobaciones. Solo devuelve solicitudes de los
    --              clientes permitidos (FN_FMI_BANDEJA_CLIENTES_PERMITIDOS):
    --              todos los de la empresa para ADMIN, los asignados para gestores.
    --              @BUSQUEDA filtra dentro de ese alcance, junto con cliente,
    --              tipo y estado, antes de contar y paginar.
    -- ========================================
    SET NOCOUNT ON;

    DECLARE @TOTAL_ELEMENTOS INT = 0;
    DECLARE @TOTAL_PAGINAS   INT = 0;
    DECLARE @ID_BUSQUEDA     INT = NULL;
    DECLARE @PATRON          NVARCHAR(210) = NULL;

    SET @NUM_PAG  = CASE WHEN @NUM_PAG IS NULL OR @NUM_PAG < 1 THEN 1 ELSE @NUM_PAG END;
    SET @CANT_PAG = CASE WHEN @CANT_PAG IS NULL OR @CANT_PAG < 1 THEN 10
                         WHEN @CANT_PAG > 50 THEN 50 ELSE @CANT_PAG END;
    SET @TIPO_SOLICITUD = NULLIF(UPPER(LTRIM(RTRIM(@TIPO_SOLICITUD))), '''');
    SET @ESTADO         = NULLIF(UPPER(LTRIM(RTRIM(@ESTADO))), '''');
    SET @BUSQUEDA       = NULLIF(LTRIM(RTRIM(@BUSQUEDA)), N'''');

    IF @BUSQUEDA IS NOT NULL
    BEGIN
        -- Número de solicitud: igualdad exacta, solo si el texto son dígitos.
        IF @BUSQUEDA NOT LIKE N''%[^0-9]%''
            SET @ID_BUSQUEDA = TRY_CONVERT(INT, @BUSQUEDA);
        -- Texto literal: %, _ y [ escritos por el usuario no actúan como comodines.
        SET @PATRON = N''%''
            + REPLACE(REPLACE(REPLACE(REPLACE(@BUSQUEDA, N''\'', N''\\''), N''%'', N''\%''), N''_'', N''\_''), N''['', N''\['')
            + N''%'';
    END;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_BANDEJA_ACCESO(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para visualizar la bandeja de aprobaciones.'' AS MENSAJE,
                   0 AS TOTAL_ELEMENTOS, 0 AS TOTAL_PAGINAS, @CANT_PAG AS TAMANIO_PAGINA;
            RETURN;
        END;

        IF (@TIPO_SOLICITUD IS NOT NULL AND @TIPO_SOLICITUD NOT IN (''INGRESO'', ''MOVIMIENTO'', ''CESE''))
           OR (@ESTADO IS NOT NULL AND @ESTADO NOT IN (''PENDIENTE'', ''APROBADA'', ''RECHAZADA'', ''ANULADA''))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''El tipo o el estado de solicitud no es válido.'' AS MENSAJE,
                   0 AS TOTAL_ELEMENTOS, 0 AS TOTAL_PAGINAS, @CANT_PAG AS TAMANIO_PAGINA;
            RETURN;
        END;

        SELECT ID_CLIENTE, RAZON_SOCIAL
        INTO #CLIENTES
        FROM dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES);

        -- Un cliente no asignado en @ID_CLIENTE no amplía el alcance: el JOIN
        -- con #CLIENTES lo deja sin resultados. El JOIN con USUARIOS limita al
        -- solicitante de la empresa del token (separación por empresa). La
        -- búsqueda solo restringe dentro de ese alcance, antes de paginar, y
        -- no distingue mayúsculas ni tildes.
        SELECT @TOTAL_ELEMENTOS = COUNT(*)
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN #CLIENTES C ON C.ID_CLIENTE = S.ID_CLIENTE
        INNER JOIN dbo.USUARIOS U
            ON U.ID_USUARIO = S.ID_USUARIO_SOLICITANTE
           AND U.ID_EMPRESA = @ID_EMPRESA
        WHERE S.ID_ESTADO_REGISTRO = 1
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND (@ID_CLIENTE IS NULL OR S.ID_CLIENTE = @ID_CLIENTE)
          AND (@TIPO_SOLICITUD IS NULL OR S.TIPO_SOLICITUD = @TIPO_SOLICITUD)
          AND (@ESTADO IS NULL OR S.ESTADO = @ESTADO)
          AND (@BUSQUEDA IS NULL
               OR S.ID_SOLICITUD = @ID_BUSQUEDA
               OR S.NOMBRE_TALENTO COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\''
               OR S.RESUMEN COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\'');

        SET @TOTAL_PAGINAS = CEILING(CAST(@TOTAL_ELEMENTOS AS FLOAT) / @CANT_PAG);

        -- MENSAJE
        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Consulta realizada correctamente.'' AS MENSAJE,
               @TOTAL_ELEMENTOS AS TOTAL_ELEMENTOS, @TOTAL_PAGINAS AS TOTAL_PAGINAS,
               @CANT_PAG AS TAMANIO_PAGINA;

        -- PAGINA SOLICITADA
        SELECT S.ID_SOLICITUD,
               S.TIPO_SOLICITUD,
               S.ESTADO,
               S.ID_CLIENTE,
               C.RAZON_SOCIAL AS CLIENTE,
               S.ID_TALENTO,
               S.NOMBRE_TALENTO,
               S.RESUMEN,
               U.USUARIO AS USUARIO_SOLICITANTE,
               CONVERT(VARCHAR(16), S.FCHCRE, 120) AS FECHA_SOLICITUD
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN #CLIENTES C ON C.ID_CLIENTE = S.ID_CLIENTE
        INNER JOIN dbo.USUARIOS U
            ON U.ID_USUARIO = S.ID_USUARIO_SOLICITANTE
           AND U.ID_EMPRESA = @ID_EMPRESA
        WHERE S.ID_ESTADO_REGISTRO = 1
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND (@ID_CLIENTE IS NULL OR S.ID_CLIENTE = @ID_CLIENTE)
          AND (@TIPO_SOLICITUD IS NULL OR S.TIPO_SOLICITUD = @TIPO_SOLICITUD)
          AND (@ESTADO IS NULL OR S.ESTADO = @ESTADO)
          AND (@BUSQUEDA IS NULL
               OR S.ID_SOLICITUD = @ID_BUSQUEDA
               OR S.NOMBRE_TALENTO COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\''
               OR S.RESUMEN COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\'')
        ORDER BY S.FCHCRE DESC, S.ID_SOLICITUD DESC
        OFFSET (@NUM_PAG - 1) * @CANT_PAG ROWS
        FETCH NEXT @CANT_PAG ROWS ONLY;

        -- CLIENTES PERMITIDOS (opciones del filtro)
        SELECT ID_CLIENTE, RAZON_SOCIAL
        FROM #CLIENTES
        ORDER BY RAZON_SOCIAL;
    END TRY
    BEGIN CATCH
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               0 AS TOTAL_ELEMENTOS, 0 AS TOTAL_PAGINAS, @CANT_PAG AS TAMANIO_PAGINA;
    END CATCH;
END;
-- FTBO1124-alcance-v2',CHAR(13),'') THROW 52240,'Cambio concurrente en SP_FMI_SOLICITUD_APROBACION_LST; revisar antes de instalar',1;
IF REPLACE(OBJECT_DEFINITION(OBJECT_ID('dbo.SP_FMI_SOLICITUD_APROBACION_CORREO_REG')),CHAR(13),'')<>REPLACE(N'
CREATE PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_CORREO_REG
(
    @ID_SOLICITUD INT,
    @TIPO_CORREO  VARCHAR(20),
    -- RESERVAR: antes de enviar. RESULTADO: después, con @ESTADO_ENVIO.
    @MODO         VARCHAR(10),
    @ESTADO_ENVIO VARCHAR(20)    = NULL,
    @DETALLE      NVARCHAR(1000) = NULL,
    @ID_USUARIO   INT            = NULL,
    @USUARIO      VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Evita correos duplicados. RESERVAR devuelve RESERVADO = 1
    --              solo a un llamador: si no hay registro, o si el anterior
    --              terminó FALLIDO u OMITIDO. RESULTADO cierra la reserva y lo
    --              anota en la línea de tiempo.
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @RESERVA TABLE (ID_SOLICITUD INT NOT NULL);

    BEGIN TRY
        IF @TIPO_CORREO NOT IN (''GESTORES'', ''AREAS'') OR @MODO NOT IN (''RESERVAR'', ''RESULTADO'')
           OR (@MODO = ''RESULTADO'' AND ISNULL(@ESTADO_ENVIO, '''') NOT IN (''ENVIADO'', ''OMITIDO'', ''FALLIDO''))
           OR NOT EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION WHERE ID_SOLICITUD = @ID_SOLICITUD)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''Parámetros de correo no válidos.'' AS MENSAJE, CAST(0 AS BIT) AS RESERVADO;
            RETURN;
        END;

        BEGIN TRANSACTION;

        IF @MODO = ''RESERVAR''
        BEGIN
            UPDATE dbo.FMI_SOLICITUD_APROBACION_CORREO WITH (HOLDLOCK)
               SET ESTADO_ENVIO = ''EN_PROCESO'', DETALLE = NULL, FCHMOD = GETDATE()
            OUTPUT INSERTED.ID_SOLICITUD INTO @RESERVA (ID_SOLICITUD)
            WHERE ID_SOLICITUD = @ID_SOLICITUD AND TIPO_CORREO = @TIPO_CORREO
              AND ESTADO_ENVIO IN (''FALLIDO'', ''OMITIDO'');

            IF NOT EXISTS (SELECT 1 FROM @RESERVA)
               AND NOT EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION_CORREO WITH (UPDLOCK, HOLDLOCK)
                               WHERE ID_SOLICITUD = @ID_SOLICITUD AND TIPO_CORREO = @TIPO_CORREO)
                INSERT INTO dbo.FMI_SOLICITUD_APROBACION_CORREO
                    (ID_SOLICITUD, TIPO_CORREO, ESTADO_ENVIO, USUCRE, FCHCRE)
                OUTPUT INSERTED.ID_SOLICITUD INTO @RESERVA (ID_SOLICITUD)
                VALUES (@ID_SOLICITUD, @TIPO_CORREO, ''EN_PROCESO'', @USUARIO, GETDATE());

            COMMIT TRANSACTION;
            SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
                   CASE WHEN EXISTS (SELECT 1 FROM @RESERVA) THEN ''Envío reservado.''
                        ELSE ''El correo ya se envió o se está enviando.'' END AS MENSAJE,
                   CAST(CASE WHEN EXISTS (SELECT 1 FROM @RESERVA) THEN 1 ELSE 0 END AS BIT) AS RESERVADO;
            RETURN;
        END;

        UPDATE dbo.FMI_SOLICITUD_APROBACION_CORREO
           SET ESTADO_ENVIO = @ESTADO_ENVIO, DETALLE = @DETALLE, FCHMOD = GETDATE()
        OUTPUT INSERTED.ID_SOLICITUD INTO @RESERVA (ID_SOLICITUD)
        WHERE ID_SOLICITUD = @ID_SOLICITUD AND TIPO_CORREO = @TIPO_CORREO
          AND ESTADO_ENVIO = ''EN_PROCESO'';

        IF EXISTS (SELECT 1 FROM @RESERVA)
            INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
                (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
            SELECT @ID_SOLICITUD, ''CORREO_'' + @ESTADO_ENVIO, S.ESTADO, S.ESTADO,
                   LEFT(N''Correo a '' + CASE @TIPO_CORREO WHEN ''GESTORES'' THEN N''gestores'' ELSE N''áreas'' END
                        + ISNULL(N'': '' + @DETALLE, N''''), 2000),
                   @ID_USUARIO, @USUARIO, GETDATE()
            FROM dbo.FMI_SOLICITUD_APROBACION S
            WHERE S.ID_SOLICITUD = @ID_SOLICITUD;

        COMMIT TRANSACTION;
        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Resultado de correo registrado.'' AS MENSAJE,
               CAST(CASE WHEN EXISTS (SELECT 1 FROM @RESERVA) THEN 1 ELSE 0 END AS BIT) AS RESERVADO;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               CAST(0 AS BIT) AS RESERVADO;
    END CATCH;
END;',CHAR(13),'') THROW 52240,'Cambio concurrente en SP_FMI_SOLICITUD_APROBACION_CORREO_REG; revisar antes de instalar',1;
IF REPLACE(OBJECT_DEFINITION(OBJECT_ID('dbo.SP_FMI_SOLICITUD_APROBACION_APR')),CHAR(13),'')<>REPLACE(N'CREATE PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_APR
(
    @ID_SOLICITUD INT,
    @VERSION      BIGINT,
    @ID_USUARIO   INT,
    @ID_EMPRESA   INT,
    @ID_ROLES     VARCHAR(100),
    @USUARIO      VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Aprueba el flujo completo. El UPDATE condicionado a
    --              ESTADO = PENDIENTE y a la versión leída hace que, entre
    --              aprobaciones simultáneas, solo una pase; las demás reciben
    --              CONFLICTO con quién aprobó. Devuelve el formulario aprobado
    --              para que el backend ejecute la operación.
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @APROBADA TABLE (
        TIPO_SOLICITUD   VARCHAR(20)   NOT NULL,
        ID_CLIENTE       INT           NOT NULL,
        ID_TALENTO       INT           NULL,
        NOMBRE_TALENTO   NVARCHAR(200) NULL,
        DATOS_FORMULARIO NVARCHAR(MAX) NOT NULL);
    DECLARE @ESTADO VARCHAR(20), @ID_CLIENTE INT, @MENSAJE NVARCHAR(400);

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_BANDEJA_ACCESO(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para visualizar la bandeja de aprobaciones.'' AS MENSAJE;
            RETURN;
        END;

        SELECT @ESTADO = S.ESTADO, @ID_CLIENTE = S.ID_CLIENTE
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) C
            ON C.ID_CLIENTE = S.ID_CLIENTE
        WHERE S.ID_SOLICITUD = @ID_SOLICITUD
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND S.ID_ESTADO_REGISTRO = 1;

        IF @ESTADO IS NULL
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''NO_ENCONTRADO'' AS CODIGO_RESULTADO,
                   ''La solicitud no existe o no está asignada a sus clientes.'' AS MENSAJE;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_APROBACION_CLIENTES_GESTIONABLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES)
                       WHERE ID_CLIENTE = @ID_CLIENTE)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''Solo un gestor asignado al cliente puede aprobar esta solicitud.'' AS MENSAJE;
            RETURN;
        END;

        BEGIN TRANSACTION;

        UPDATE dbo.FMI_SOLICITUD_APROBACION
           SET ESTADO = ''APROBADA'',
               USUMOD = @USUARIO,
               FCHMOD = GETDATE()
        OUTPUT INSERTED.TIPO_SOLICITUD, INSERTED.ID_CLIENTE, INSERTED.ID_TALENTO,
               INSERTED.NOMBRE_TALENTO, INSERTED.DATOS_FORMULARIO
          INTO @APROBADA (TIPO_SOLICITUD, ID_CLIENTE, ID_TALENTO, NOMBRE_TALENTO, DATOS_FORMULARIO)
        WHERE ID_SOLICITUD = @ID_SOLICITUD
          AND ESTADO = ''PENDIENTE''
          AND CONVERT(BIGINT, VERSION_FILA) = @VERSION;

        IF NOT EXISTS (SELECT 1 FROM @APROBADA)
        BEGIN
            ROLLBACK TRANSACTION;
            -- Estado actual, ya con el bloqueo liberado por quien ganó.
            SELECT @ESTADO = ESTADO FROM dbo.FMI_SOLICITUD_APROBACION WHERE ID_SOLICITUD = @ID_SOLICITUD;
            IF @ESTADO = ''APROBADA''
                SELECT TOP (1) @MENSAJE = N''La solicitud ya fue aprobada por '' + H.USUARIO
                                          + N'' el '' + CONVERT(VARCHAR(16), H.FCHCRE, 120) + N''.''
                FROM dbo.FMI_SOLICITUD_APROBACION_HIST H
                WHERE H.ID_SOLICITUD = @ID_SOLICITUD AND H.ACCION = ''APROBADA''
                ORDER BY H.ID_HISTORIAL DESC;
            SELECT 1 AS ID_TIPO_MENSAJE, ''CONFLICTO'' AS CODIGO_RESULTADO,
                   COALESCE(@MENSAJE,
                            CASE WHEN @ESTADO <> ''PENDIENTE''
                                 THEN N''La solicitud ya no está pendiente.''
                                 ELSE N''Otra persona modificó la solicitud. Recárguela y revise los cambios antes de aprobar.'' END) AS MENSAJE;
            RETURN;
        END;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
            (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
        VALUES
            (@ID_SOLICITUD, ''APROBADA'', ''PENDIENTE'', ''APROBADA'', N''Flujo completo aprobado.'', @ID_USUARIO, @USUARIO, GETDATE());

        COMMIT TRANSACTION;

        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Solicitud aprobada.'' AS MENSAJE;

        SELECT @ID_SOLICITUD AS ID_SOLICITUD, TIPO_SOLICITUD, ID_CLIENTE, ID_TALENTO,
               NOMBRE_TALENTO, DATOS_FORMULARIO
        FROM @APROBADA;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE;
    END CATCH;
END;
-- FTBO1124-alcance-v2',CHAR(13),'') THROW 52240,'Cambio concurrente en SP_FMI_SOLICITUD_APROBACION_APR; revisar antes de instalar',1;
IF REPLACE(OBJECT_DEFINITION(OBJECT_ID('dbo.SP_FMI_SOLICITUD_APROBACION_UPD')),CHAR(13),'')<>REPLACE(N'CREATE PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_UPD
(
    @ID_SOLICITUD     INT,
    @VERSION          BIGINT,
    @DATOS_FORMULARIO NVARCHAR(MAX),
    @NOMBRE_TALENTO   NVARCHAR(200) = NULL,
    -- Campos cambiados, para el historial (lo arma el backend).
    @DETALLE          NVARCHAR(2000) = NULL,
    @ID_USUARIO       INT,
    @ID_EMPRESA       INT,
    @ID_ROLES         VARCHAR(100),
    @USUARIO          VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Guarda el formulario editado por un gestor asignado, solo
    --              si la solicitud sigue PENDIENTE y nadie la cambió antes.
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @CAMBIADA TABLE (VERSION BIGINT NOT NULL);
    DECLARE @ESTADO VARCHAR(20), @ID_CLIENTE INT;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_BANDEJA_ACCESO(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para visualizar la bandeja de aprobaciones.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        SELECT @ESTADO = S.ESTADO, @ID_CLIENTE = S.ID_CLIENTE
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) C
            ON C.ID_CLIENTE = S.ID_CLIENTE
        WHERE S.ID_SOLICITUD = @ID_SOLICITUD
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND S.ID_ESTADO_REGISTRO = 1;

        IF @ESTADO IS NULL
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''NO_ENCONTRADO'' AS CODIGO_RESULTADO,
                   ''La solicitud no existe o no está asignada a sus clientes.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_APROBACION_CLIENTES_GESTIONABLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES)
                       WHERE ID_CLIENTE = @ID_CLIENTE)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''Solo un gestor asignado al cliente puede editar esta solicitud.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        IF @DATOS_FORMULARIO IS NULL OR ISJSON(@DATOS_FORMULARIO) <> 1
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''Los datos del formulario no son válidos.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        BEGIN TRANSACTION;

        UPDATE dbo.FMI_SOLICITUD_APROBACION
           SET DATOS_FORMULARIO = @DATOS_FORMULARIO,
               NOMBRE_TALENTO   = COALESCE(@NOMBRE_TALENTO, NOMBRE_TALENTO),
               USUMOD           = @USUARIO,
               FCHMOD           = GETDATE()
        OUTPUT CONVERT(BIGINT, INSERTED.VERSION_FILA) INTO @CAMBIADA (VERSION)
        WHERE ID_SOLICITUD = @ID_SOLICITUD
          AND ESTADO = ''PENDIENTE''
          AND CONVERT(BIGINT, VERSION_FILA) = @VERSION;

        IF NOT EXISTS (SELECT 1 FROM @CAMBIADA)
        BEGIN
            ROLLBACK TRANSACTION;
            SELECT 1 AS ID_TIPO_MENSAJE, ''CONFLICTO'' AS CODIGO_RESULTADO,
                   CASE WHEN @ESTADO <> ''PENDIENTE''
                        THEN ''La solicitud ya no está pendiente: no se puede editar.''
                        ELSE ''Otra persona modificó la solicitud. Recárguela antes de guardar.'' END AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
            (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
        VALUES
            (@ID_SOLICITUD, ''FORMULARIO_EDITADO'', ''PENDIENTE'', ''PENDIENTE'', @DETALLE, @ID_USUARIO, @USUARIO, GETDATE());

        COMMIT TRANSACTION;

        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Formulario guardado correctamente.'' AS MENSAJE,
               (SELECT VERSION FROM @CAMBIADA) AS VERSION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               CAST(NULL AS BIGINT) AS VERSION;
    END CATCH;
END;
-- FTBO1124-alcance-v2',CHAR(13),'') THROW 52240,'Cambio concurrente en SP_FMI_SOLICITUD_APROBACION_UPD; revisar antes de instalar',1;
BEGIN TRANSACTION;
IF OBJECT_ID('dbo.FMI_SOLICITUD_APROBACION_AREA') IS NULL
CREATE TABLE dbo.FMI_SOLICITUD_APROBACION_AREA (
 ID_SOLICITUD INT NOT NULL REFERENCES dbo.FMI_SOLICITUD_APROBACION(ID_SOLICITUD),
 AREA VARCHAR(20) NOT NULL CHECK (AREA IN ('FMI','EQUIPO','CREDENCIALES')),
 ESTADO VARCHAR(20) NOT NULL CHECK (ESTADO IN ('PENDIENTE','APROBADA','RECHAZADA','NO_APLICA')),
 USUARIO VARCHAR(200) NULL, FCHRESOLUCION DATETIME NULL,
 PRIMARY KEY (ID_SOLICITUD,AREA)
);
IF OBJECT_ID('dbo.FMI_SOLICITUD_APROBACION_REVISION') IS NULL
CREATE TABLE dbo.FMI_SOLICITUD_APROBACION_REVISION (
 ID_SOLICITUD INT NOT NULL REFERENCES dbo.FMI_SOLICITUD_APROBACION(ID_SOLICITUD),
 ID_USUARIO INT NOT NULL, AREA VARCHAR(20) NOT NULL CHECK (AREA IN ('FMI','EQUIPO','CREDENCIALES')),
 VERSION BIGINT NOT NULL, PRIMARY KEY (ID_SOLICITUD,ID_USUARIO,AREA)
);
ALTER TABLE dbo.FMI_SOLICITUD_APROBACION DROP CONSTRAINT CK_FMI_SOLICITUD_APROBACION_TIPO;
ALTER TABLE dbo.FMI_SOLICITUD_APROBACION WITH CHECK ADD CONSTRAINT CK_FMI_SOLICITUD_APROBACION_TIPO CHECK (TIPO_SOLICITUD IN ('INGRESO', 'MOVIMIENTO', 'CESE', 'ASIGNACION_EQUIPO', 'CREDENCIALES'));
ALTER TABLE dbo.FMI_SOLICITUD_APROBACION_HIST DROP CONSTRAINT CK_FMI_SOLICITUD_APROBACION_HIST_ACCION;
ALTER TABLE dbo.FMI_SOLICITUD_APROBACION_HIST WITH CHECK ADD CONSTRAINT CK_FMI_SOLICITUD_APROBACION_HIST_ACCION CHECK (([ACCION]='CORREO_FALLIDO' OR [ACCION]='CORREO_OMITIDO' OR [ACCION]='CORREO_ENVIADO' OR [ACCION]='EJECUCION_INCIERTA' OR [ACCION]='EJECUCION_FALLIDA' OR [ACCION]='EJECUTADA' OR [ACCION]='APROBADA' OR [ACCION]='FORMULARIO_EDITADO' OR [ACCION]='CREADA') OR ACCION IN ('AREA_APROBADA','AREA_RECHAZADA','RESUELTA'));
ALTER TABLE dbo.FMI_SOLICITUD_APROBACION_CORREO DROP CONSTRAINT CK_FMI_SOLICITUD_APROBACION_CORREO_TIPO;
ALTER TABLE dbo.FMI_SOLICITUD_APROBACION_CORREO WITH CHECK ADD CONSTRAINT CK_FMI_SOLICITUD_APROBACION_CORREO_TIPO CHECK (TIPO_CORREO IN ('GESTORES', 'AREAS', 'AREA_FMI', 'AREA_EQUIPO', 'AREA_CREDENCIALES', 'RECHAZO_FMI', 'RECHAZO_EQUIPO', 'RECHAZO_CREDENCIALES'));
EXEC sys.sp_executesql N'ALTER PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_REG
(
    @TIPO_SOLICITUD   VARCHAR(20),
    @ID_CLIENTE       INT,
    @ID_TALENTO       INT           = NULL,
    @NOMBRE_TALENTO   NVARCHAR(200) = NULL,
    @RESUMEN          NVARCHAR(300) = NULL,
    @DATOS_FORMULARIO NVARCHAR(MAX),
    -- IDENTIDAD DEL SOLICITANTE (del token validado por el backend)
    @ID_USUARIO         INT,
    @ID_EMPRESA         INT,
    @ID_ROLES           VARCHAR(100),
    -- Funcionalidades que exige la operación, como en el registro directo ("3,5").
    @ID_FUNCIONALIDADES VARCHAR(100),
    @USUARIO            VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Registra una solicitud PENDIENTE con su primer evento del
    --              historial y devuelve los gestores activos del cliente. No
    --              ejecuta la operación ni envía correos. El solicitante debe
    --              tener, en uno de sus roles vigentes del token, todas las
    --              funcionalidades de la operación (ROL_FUNCIONALIDAD).
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    SET @TIPO_SOLICITUD = UPPER(LTRIM(RTRIM(@TIPO_SOLICITUD)));
    DECLARE @ID_SOLICITUD INT, @ID_PENDIENTE INT;
    DECLARE @NUEVA TABLE (ID_SOLICITUD INT NOT NULL);
    DECLARE @ROLES VARCHAR(102) = '','' + REPLACE(ISNULL(@ID_ROLES, ''''), '' '', '''') + '','';
    DECLARE @FUNCS VARCHAR(100) = REPLACE(ISNULL(@ID_FUNCIONALIDADES, ''''), '' '', '''');

    BEGIN TRY
        IF @FUNCS = '''' OR NOT EXISTS (
            SELECT 1
            FROM dbo.USUARIO_ROL UR
            WHERE UR.ID_USUARIO = @ID_USUARIO
              AND UR.ID_ESTADO_REGISTRO = 1
              AND @ROLES LIKE ''%,'' + CONVERT(VARCHAR(11), UR.ID_TIPO_ROL) + '',%''
              AND (SELECT COUNT(DISTINCT RFN.ID_FUNCIONALIDAD)
                   FROM dbo.ROL_FUNCIONALIDAD RFN
                   WHERE RFN.ID_TIPO_ROL = UR.ID_TIPO_ROL
                     AND RFN.ID_EMPRESA = @ID_EMPRESA
                     AND RFN.ID_ESTADO_REGISTRO = 1
                     AND '','' + @FUNCS + '','' LIKE ''%,'' + CONVERT(VARCHAR(11), RFN.ID_FUNCIONALIDAD) + '',%'')
                  = LEN(@FUNCS) - LEN(REPLACE(@FUNCS, '','', '''')) + 1)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para registrar esta operación.'' AS MENSAJE,
                   CAST(NULL AS INT) AS ID_SOLICITUD;
            RETURN;
        END;

        IF @TIPO_SOLICITUD IS NULL OR @TIPO_SOLICITUD NOT IN (''INGRESO'', ''MOVIMIENTO'', ''CESE'', ''ASIGNACION_EQUIPO'', ''CREDENCIALES'')
           OR @DATOS_FORMULARIO IS NULL OR ISJSON(@DATOS_FORMULARIO) <> 1
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''Tipo de solicitud o datos del formulario no válidos.'' AS MENSAJE,
                   CAST(NULL AS INT) AS ID_SOLICITUD;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.USUARIOS
                       WHERE ID_USUARIO = @ID_USUARIO AND ID_EMPRESA = @ID_EMPRESA
                         AND ID_ESTADO_REGISTRO = 1)
           OR NOT EXISTS (SELECT 1 FROM dbo.CLIENTE
                          WHERE ID_CLIENTE = @ID_CLIENTE AND ID_EMPRESA = @ID_EMPRESA
                            AND ID_ESTADO_REGISTRO = 1)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''El usuario o el cliente no están activos en la empresa.'' AS MENSAJE,
                   CAST(NULL AS INT) AS ID_SOLICITUD;
            RETURN;
        END;

        BEGIN TRANSACTION;

        -- Un mismo talento no puede tener dos solicitudes pendientes del mismo
        -- tipo en el mismo cliente (doble clic, reenvío del formulario).
        IF @ID_TALENTO IS NOT NULL
            SELECT TOP (1) @ID_PENDIENTE = S.ID_SOLICITUD
            FROM dbo.FMI_SOLICITUD_APROBACION S WITH (UPDLOCK, HOLDLOCK)
            WHERE S.ID_TALENTO = @ID_TALENTO
              AND S.ID_CLIENTE = @ID_CLIENTE
              AND S.TIPO_SOLICITUD = @TIPO_SOLICITUD
              AND S.ESTADO = ''PENDIENTE''
              AND S.ID_ESTADO_REGISTRO = 1;

        IF @ID_PENDIENTE IS NOT NULL
        BEGIN
            COMMIT TRANSACTION;
            SELECT 1 AS ID_TIPO_MENSAJE, ''CONFLICTO'' AS CODIGO_RESULTADO,
                   ''Ya existe la solicitud pendiente N° '' + CAST(@ID_PENDIENTE AS VARCHAR(11))
                   + '' para este talento.'' AS MENSAJE,
                   @ID_PENDIENTE AS ID_SOLICITUD;
            RETURN;
        END;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION
            (ORIGEN_FLUJO, TIPO_SOLICITUD, ESTADO, ID_CLIENTE, ID_USUARIO_SOLICITANTE, ID_TALENTO,
             NOMBRE_TALENTO, RESUMEN, DATOS_FORMULARIO, USUCRE, FCHCRE, ID_ESTADO_REGISTRO)
        OUTPUT INSERTED.ID_SOLICITUD INTO @NUEVA (ID_SOLICITUD)
        VALUES
            (''FTBO-1124'', @TIPO_SOLICITUD, ''PENDIENTE'', @ID_CLIENTE, @ID_USUARIO, @ID_TALENTO,
             @NOMBRE_TALENTO, @RESUMEN, @DATOS_FORMULARIO, @USUARIO, GETDATE(), 1);

        SELECT @ID_SOLICITUD = ID_SOLICITUD FROM @NUEVA;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
            (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
        VALUES
            (@ID_SOLICITUD, ''CREADA'', NULL, ''PENDIENTE'', @RESUMEN, @ID_USUARIO, @USUARIO, GETDATE());

        COMMIT TRANSACTION;

        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Solicitud registrada correctamente.'' AS MENSAJE,
               @ID_SOLICITUD AS ID_SOLICITUD,
               (SELECT MIN(RAZON_SOCIAL) FROM dbo.CLIENTE
                WHERE ID_CLIENTE = @ID_CLIENTE AND ID_EMPRESA = @ID_EMPRESA) AS CLIENTE;

        -- GESTORES ACTIVOS DEL CLIENTE
        SELECT U.ID_USUARIO, U.USUARIO, MIN(UR.ID_TIPO_ROL) AS ID_TIPO_ROL
        FROM dbo.CLIENTE_GESTOR CG
        INNER JOIN dbo.USUARIOS U
            ON U.ID_USUARIO = CG.ID_USUARIO
           AND U.ID_EMPRESA = @ID_EMPRESA
           AND U.ID_ESTADO_REGISTRO = 1
        INNER JOIN dbo.USUARIO_ROL UR
            ON UR.ID_USUARIO = U.ID_USUARIO
           AND UR.ID_ESTADO_REGISTRO = 1
        WHERE CG.ID_CLIENTE = @ID_CLIENTE
          AND CG.ID_ESTADO_REGISTRO = 1
        GROUP BY U.ID_USUARIO, U.USUARIO;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               CAST(NULL AS INT) AS ID_SOLICITUD;
    END CATCH;
END;
-- FTBO1124-alcance-v2';
EXEC sys.sp_executesql N'ALTER PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_LST
(
    -- FILTROS
    @ID_CLIENTE     INT          = NULL,
    @TIPO_SOLICITUD VARCHAR(20)  = NULL,
    @ESTADO         VARCHAR(20)  = NULL,
    -- Número exacto de solicitud, o texto contenido en el nombre del talento
    -- o en el resumen. Se usa solo como valor (LIKE con comodines escapados).
    @BUSQUEDA       NVARCHAR(100) = NULL,
    -- PAGINACION
    @NUM_PAG        INT          = 1,
    @CANT_PAG       INT          = 10,
    -- IDENTIDAD (del token validado por el backend)
    @ID_USUARIO     INT,
    @ID_EMPRESA     INT,
    @ID_ROLES       VARCHAR(100),
    @USUARIO        VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1118
    -- Descripción: Bandeja de aprobaciones. Solo devuelve solicitudes de los
    --              clientes permitidos (FN_FMI_BANDEJA_CLIENTES_PERMITIDOS):
    --              todos los de la empresa para ADMIN, los asignados para gestores.
    --              @BUSQUEDA filtra dentro de ese alcance, junto con cliente,
    --              tipo y estado, antes de contar y paginar.
    -- ========================================
    SET NOCOUNT ON;

    DECLARE @TOTAL_ELEMENTOS INT = 0;
    DECLARE @TOTAL_PAGINAS   INT = 0;
    DECLARE @ID_BUSQUEDA     INT = NULL;
    DECLARE @PATRON          NVARCHAR(210) = NULL;

    SET @NUM_PAG  = CASE WHEN @NUM_PAG IS NULL OR @NUM_PAG < 1 THEN 1 ELSE @NUM_PAG END;
    SET @CANT_PAG = CASE WHEN @CANT_PAG IS NULL OR @CANT_PAG < 1 THEN 10
                         WHEN @CANT_PAG > 50 THEN 50 ELSE @CANT_PAG END;
    SET @TIPO_SOLICITUD = NULLIF(UPPER(LTRIM(RTRIM(@TIPO_SOLICITUD))), '''');
    SET @ESTADO         = NULLIF(UPPER(LTRIM(RTRIM(@ESTADO))), '''');
    SET @BUSQUEDA       = NULLIF(LTRIM(RTRIM(@BUSQUEDA)), N'''');
    IF @BUSQUEDA IS NULL SET @ESTADO = ''PENDIENTE'';

    IF @BUSQUEDA IS NOT NULL
    BEGIN
        -- Número de solicitud: igualdad exacta, solo si el texto son dígitos.
        IF @BUSQUEDA NOT LIKE N''%[^0-9]%''
            SET @ID_BUSQUEDA = TRY_CONVERT(INT, @BUSQUEDA);
        -- Texto literal: %, _ y [ escritos por el usuario no actúan como comodines.
        SET @PATRON = N''%''
            + REPLACE(REPLACE(REPLACE(REPLACE(@BUSQUEDA, N''\'', N''\\''), N''%'', N''\%''), N''_'', N''\_''), N''['', N''\['')
            + N''%'';
    END;

    BEGIN TRY
        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_BANDEJA_ACCESO(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para visualizar la bandeja de aprobaciones.'' AS MENSAJE,
                   0 AS TOTAL_ELEMENTOS, 0 AS TOTAL_PAGINAS, @CANT_PAG AS TAMANIO_PAGINA;
            RETURN;
        END;

        IF (@TIPO_SOLICITUD IS NOT NULL AND @TIPO_SOLICITUD NOT IN (''INGRESO'', ''MOVIMIENTO'', ''CESE'', ''ASIGNACION_EQUIPO'', ''CREDENCIALES''))
           OR (@ESTADO IS NOT NULL AND @ESTADO NOT IN (''PENDIENTE'', ''APROBADA'', ''RECHAZADA'', ''ANULADA''))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''El tipo o el estado de solicitud no es válido.'' AS MENSAJE,
                   0 AS TOTAL_ELEMENTOS, 0 AS TOTAL_PAGINAS, @CANT_PAG AS TAMANIO_PAGINA;
            RETURN;
        END;

        SELECT ID_CLIENTE, RAZON_SOCIAL
        INTO #CLIENTES
        FROM dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES);

        -- Un cliente no asignado en @ID_CLIENTE no amplía el alcance: el JOIN
        -- con #CLIENTES lo deja sin resultados. El JOIN con USUARIOS limita al
        -- solicitante de la empresa del token (separación por empresa). La
        -- búsqueda solo restringe dentro de ese alcance, antes de paginar, y
        -- no distingue mayúsculas ni tildes.
        SELECT @TOTAL_ELEMENTOS = COUNT(*)
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN #CLIENTES C ON C.ID_CLIENTE = S.ID_CLIENTE
        INNER JOIN dbo.USUARIOS U
            ON U.ID_USUARIO = S.ID_USUARIO_SOLICITANTE
           AND U.ID_EMPRESA = @ID_EMPRESA
        WHERE S.ID_ESTADO_REGISTRO = 1
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND (@ID_CLIENTE IS NULL OR S.ID_CLIENTE = @ID_CLIENTE)
          AND (@TIPO_SOLICITUD IS NULL OR S.TIPO_SOLICITUD = @TIPO_SOLICITUD)
          AND (@ESTADO IS NULL OR S.ESTADO = @ESTADO)
          AND (@BUSQUEDA IS NULL
               OR S.ID_SOLICITUD = @ID_BUSQUEDA
               OR S.NOMBRE_TALENTO COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\''
               OR S.RESUMEN COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\'');

        SET @TOTAL_PAGINAS = CEILING(CAST(@TOTAL_ELEMENTOS AS FLOAT) / @CANT_PAG);

        -- MENSAJE
        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Consulta realizada correctamente.'' AS MENSAJE,
               @TOTAL_ELEMENTOS AS TOTAL_ELEMENTOS, @TOTAL_PAGINAS AS TOTAL_PAGINAS,
               @CANT_PAG AS TAMANIO_PAGINA;

        -- PAGINA SOLICITADA
        SELECT S.ID_SOLICITUD,
               S.TIPO_SOLICITUD,
               S.ESTADO,
               S.ID_CLIENTE,
               C.RAZON_SOCIAL AS CLIENTE,
               S.ID_TALENTO,
               S.NOMBRE_TALENTO,
               S.RESUMEN,
               U.USUARIO AS USUARIO_SOLICITANTE,
               CONVERT(VARCHAR(16), S.FCHCRE, 120) AS FECHA_SOLICITUD,
               CASE WHEN S.ESTADO<>''PENDIENTE'' THEN (SELECT CONVERT(VARCHAR(19),MAX(H.FCHCRE),120) FROM dbo.FMI_SOLICITUD_APROBACION_HIST H WHERE H.ID_SOLICITUD=S.ID_SOLICITUD AND H.ACCION IN (''APROBADA'',''RESUELTA'') AND H.ESTADO_NUEVO=S.ESTADO) END AS FECHA_RESOLUCION
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN #CLIENTES C ON C.ID_CLIENTE = S.ID_CLIENTE
        INNER JOIN dbo.USUARIOS U
            ON U.ID_USUARIO = S.ID_USUARIO_SOLICITANTE
           AND U.ID_EMPRESA = @ID_EMPRESA
        WHERE S.ID_ESTADO_REGISTRO = 1
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND (@ID_CLIENTE IS NULL OR S.ID_CLIENTE = @ID_CLIENTE)
          AND (@TIPO_SOLICITUD IS NULL OR S.TIPO_SOLICITUD = @TIPO_SOLICITUD)
          AND (@ESTADO IS NULL OR S.ESTADO = @ESTADO)
          AND (@BUSQUEDA IS NULL
               OR S.ID_SOLICITUD = @ID_BUSQUEDA
               OR S.NOMBRE_TALENTO COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\''
               OR S.RESUMEN COLLATE Latin1_General_CI_AI LIKE @PATRON ESCAPE N''\'')
        ORDER BY S.FCHCRE DESC, S.ID_SOLICITUD DESC
        OFFSET (@NUM_PAG - 1) * @CANT_PAG ROWS
        FETCH NEXT @CANT_PAG ROWS ONLY;

        -- CLIENTES PERMITIDOS (opciones del filtro)
        SELECT ID_CLIENTE, RAZON_SOCIAL
        FROM #CLIENTES
        ORDER BY RAZON_SOCIAL;
    END TRY
    BEGIN CATCH
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               0 AS TOTAL_ELEMENTOS, 0 AS TOTAL_PAGINAS, @CANT_PAG AS TAMANIO_PAGINA;
    END CATCH;
END;
-- FTBO1124-alcance-v2';
EXEC sys.sp_executesql N'
ALTER PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_CORREO_REG
(
    @ID_SOLICITUD INT,
    @TIPO_CORREO  VARCHAR(20),
    -- RESERVAR: antes de enviar. RESULTADO: después, con @ESTADO_ENVIO.
    @MODO         VARCHAR(10),
    @ESTADO_ENVIO VARCHAR(20)    = NULL,
    @DETALLE      NVARCHAR(1000) = NULL,
    @ID_USUARIO   INT            = NULL,
    @USUARIO      VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Evita correos duplicados. RESERVAR devuelve RESERVADO = 1
    --              solo a un llamador: si no hay registro, o si el anterior
    --              terminó FALLIDO u OMITIDO. RESULTADO cierra la reserva y lo
    --              anota en la línea de tiempo.
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @RESERVA TABLE (ID_SOLICITUD INT NOT NULL);

    BEGIN TRY
        IF @TIPO_CORREO NOT IN (''GESTORES'', ''AREAS'', ''AREA_FMI'', ''AREA_EQUIPO'', ''AREA_CREDENCIALES'', ''RECHAZO_FMI'', ''RECHAZO_EQUIPO'', ''RECHAZO_CREDENCIALES'') OR @MODO NOT IN (''RESERVAR'', ''RESULTADO'')
           OR (@MODO = ''RESULTADO'' AND ISNULL(@ESTADO_ENVIO, '''') NOT IN (''ENVIADO'', ''OMITIDO'', ''FALLIDO''))
           OR NOT EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION WHERE ID_SOLICITUD = @ID_SOLICITUD)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''Parámetros de correo no válidos.'' AS MENSAJE, CAST(0 AS BIT) AS RESERVADO;
            RETURN;
        END;

        BEGIN TRANSACTION;

        IF @MODO = ''RESERVAR''
        BEGIN
            UPDATE dbo.FMI_SOLICITUD_APROBACION_CORREO WITH (HOLDLOCK)
               SET ESTADO_ENVIO = ''EN_PROCESO'', DETALLE = NULL, FCHMOD = GETDATE()
            OUTPUT INSERTED.ID_SOLICITUD INTO @RESERVA (ID_SOLICITUD)
            WHERE ID_SOLICITUD = @ID_SOLICITUD AND TIPO_CORREO = @TIPO_CORREO
              AND ESTADO_ENVIO IN (''FALLIDO'', ''OMITIDO'');

            IF NOT EXISTS (SELECT 1 FROM @RESERVA)
               AND NOT EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION_CORREO WITH (UPDLOCK, HOLDLOCK)
                               WHERE ID_SOLICITUD = @ID_SOLICITUD AND TIPO_CORREO = @TIPO_CORREO)
                INSERT INTO dbo.FMI_SOLICITUD_APROBACION_CORREO
                    (ID_SOLICITUD, TIPO_CORREO, ESTADO_ENVIO, USUCRE, FCHCRE)
                OUTPUT INSERTED.ID_SOLICITUD INTO @RESERVA (ID_SOLICITUD)
                VALUES (@ID_SOLICITUD, @TIPO_CORREO, ''EN_PROCESO'', @USUARIO, GETDATE());

            COMMIT TRANSACTION;
            SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
                   CASE WHEN EXISTS (SELECT 1 FROM @RESERVA) THEN ''Envío reservado.''
                        ELSE ''El correo ya se envió o se está enviando.'' END AS MENSAJE,
                   CAST(CASE WHEN EXISTS (SELECT 1 FROM @RESERVA) THEN 1 ELSE 0 END AS BIT) AS RESERVADO;
            RETURN;
        END;

        UPDATE dbo.FMI_SOLICITUD_APROBACION_CORREO
           SET ESTADO_ENVIO = @ESTADO_ENVIO, DETALLE = @DETALLE, FCHMOD = GETDATE()
        OUTPUT INSERTED.ID_SOLICITUD INTO @RESERVA (ID_SOLICITUD)
        WHERE ID_SOLICITUD = @ID_SOLICITUD AND TIPO_CORREO = @TIPO_CORREO
          AND ESTADO_ENVIO = ''EN_PROCESO'';

        IF EXISTS (SELECT 1 FROM @RESERVA)
            INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
                (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
            SELECT @ID_SOLICITUD, ''CORREO_'' + @ESTADO_ENVIO, S.ESTADO, S.ESTADO,
                   LEFT(N''Correo a '' + CASE @TIPO_CORREO WHEN ''GESTORES'' THEN N''gestores'' ELSE N''áreas'' END
                        + ISNULL(N'': '' + @DETALLE, N''''), 2000),
                   @ID_USUARIO, @USUARIO, GETDATE()
            FROM dbo.FMI_SOLICITUD_APROBACION S
            WHERE S.ID_SOLICITUD = @ID_SOLICITUD;

        COMMIT TRANSACTION;
        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Resultado de correo registrado.'' AS MENSAJE,
               CAST(CASE WHEN EXISTS (SELECT 1 FROM @RESERVA) THEN 1 ELSE 0 END AS BIT) AS RESERVADO;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               CAST(0 AS BIT) AS RESERVADO;
    END CATCH;
END;';
EXEC sys.sp_executesql N'ALTER PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_APR
(
    @ID_SOLICITUD INT,
    @VERSION      BIGINT,
    @ID_USUARIO   INT,
    @ID_EMPRESA   INT,
    @ID_ROLES     VARCHAR(100),
    @USUARIO      VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Aprueba el flujo completo. El UPDATE condicionado a
    --              ESTADO = PENDIENTE y a la versión leída hace que, entre
    --              aprobaciones simultáneas, solo una pase; las demás reciben
    --              CONFLICTO con quién aprobó. Devuelve el formulario aprobado
    --              para que el backend ejecute la operación.
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @APROBADA TABLE (
        TIPO_SOLICITUD   VARCHAR(20)   NOT NULL,
        ID_CLIENTE       INT           NOT NULL,
        ID_TALENTO       INT           NULL,
        NOMBRE_TALENTO   NVARCHAR(200) NULL,
        DATOS_FORMULARIO NVARCHAR(MAX) NOT NULL);
    DECLARE @ESTADO VARCHAR(20), @ID_CLIENTE INT, @MENSAJE NVARCHAR(400);

    BEGIN TRY
        IF EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION_AREA WHERE ID_SOLICITUD=@ID_SOLICITUD) OR EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION WHERE ID_SOLICITUD=@ID_SOLICITUD AND TIPO_SOLICITUD IN (''ASIGNACION_EQUIPO'',''CREDENCIALES''))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE,''CONFLICTO'' AS CODIGO_RESULTADO,N''La solicitud se gestiona por areas. Actualice la bandeja.'' AS MENSAJE;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_BANDEJA_ACCESO(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para visualizar la bandeja de aprobaciones.'' AS MENSAJE;
            RETURN;
        END;

        SELECT @ESTADO = S.ESTADO, @ID_CLIENTE = S.ID_CLIENTE
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) C
            ON C.ID_CLIENTE = S.ID_CLIENTE
        WHERE S.ID_SOLICITUD = @ID_SOLICITUD
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND S.ID_ESTADO_REGISTRO = 1;

        IF @ESTADO IS NULL
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''NO_ENCONTRADO'' AS CODIGO_RESULTADO,
                   ''La solicitud no existe o no está asignada a sus clientes.'' AS MENSAJE;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_APROBACION_CLIENTES_GESTIONABLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES)
                       WHERE ID_CLIENTE = @ID_CLIENTE)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''Solo un gestor asignado al cliente puede aprobar esta solicitud.'' AS MENSAJE;
            RETURN;
        END;

        BEGIN TRANSACTION;

        UPDATE dbo.FMI_SOLICITUD_APROBACION
           SET ESTADO = ''APROBADA'',
               USUMOD = @USUARIO,
               FCHMOD = GETDATE()
        OUTPUT INSERTED.TIPO_SOLICITUD, INSERTED.ID_CLIENTE, INSERTED.ID_TALENTO,
               INSERTED.NOMBRE_TALENTO, INSERTED.DATOS_FORMULARIO
          INTO @APROBADA (TIPO_SOLICITUD, ID_CLIENTE, ID_TALENTO, NOMBRE_TALENTO, DATOS_FORMULARIO)
        WHERE ID_SOLICITUD = @ID_SOLICITUD
          AND ESTADO = ''PENDIENTE''
          AND CONVERT(BIGINT, VERSION_FILA) = @VERSION;

        IF NOT EXISTS (SELECT 1 FROM @APROBADA)
        BEGIN
            ROLLBACK TRANSACTION;
            -- Estado actual, ya con el bloqueo liberado por quien ganó.
            SELECT @ESTADO = ESTADO FROM dbo.FMI_SOLICITUD_APROBACION WHERE ID_SOLICITUD = @ID_SOLICITUD;
            IF @ESTADO = ''APROBADA''
                SELECT TOP (1) @MENSAJE = N''La solicitud ya fue aprobada por '' + H.USUARIO
                                          + N'' el '' + CONVERT(VARCHAR(16), H.FCHCRE, 120) + N''.''
                FROM dbo.FMI_SOLICITUD_APROBACION_HIST H
                WHERE H.ID_SOLICITUD = @ID_SOLICITUD AND H.ACCION = ''APROBADA''
                ORDER BY H.ID_HISTORIAL DESC;
            SELECT 1 AS ID_TIPO_MENSAJE, ''CONFLICTO'' AS CODIGO_RESULTADO,
                   COALESCE(@MENSAJE,
                            CASE WHEN @ESTADO <> ''PENDIENTE''
                                 THEN N''La solicitud ya no está pendiente.''
                                 ELSE N''Otra persona modificó la solicitud. Recárguela y revise los cambios antes de aprobar.'' END) AS MENSAJE;
            RETURN;
        END;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
            (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
        VALUES
            (@ID_SOLICITUD, ''APROBADA'', ''PENDIENTE'', ''APROBADA'', N''Flujo completo aprobado.'', @ID_USUARIO, @USUARIO, GETDATE());

        COMMIT TRANSACTION;

        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Solicitud aprobada.'' AS MENSAJE;

        SELECT @ID_SOLICITUD AS ID_SOLICITUD, TIPO_SOLICITUD, ID_CLIENTE, ID_TALENTO,
               NOMBRE_TALENTO, DATOS_FORMULARIO
        FROM @APROBADA;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE;
    END CATCH;
END;
-- FTBO1124-alcance-v2';
EXEC sys.sp_executesql N'ALTER PROCEDURE dbo.SP_FMI_SOLICITUD_APROBACION_UPD
(
    @ID_SOLICITUD     INT,
    @VERSION          BIGINT,
    @DATOS_FORMULARIO NVARCHAR(MAX),
    @NOMBRE_TALENTO   NVARCHAR(200) = NULL,
    -- Campos cambiados, para el historial (lo arma el backend).
    @DETALLE          NVARCHAR(2000) = NULL,
    @ID_USUARIO       INT,
    @ID_EMPRESA       INT,
    @ID_ROLES         VARCHAR(100),
    @USUARIO          VARCHAR(200)
)
AS
BEGIN
    -- ========== Control de Cambios ==========
    -- Código: FTBO-1124
    -- Descripción: Guarda el formulario editado por un gestor asignado, solo
    --              si la solicitud sigue PENDIENTE y nadie la cambió antes.
    -- ========================================
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    DECLARE @CAMBIADA TABLE (VERSION BIGINT NOT NULL);
    DECLARE @ESTADO VARCHAR(20), @ID_CLIENTE INT;

    BEGIN TRY
        IF EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION_AREA WHERE ID_SOLICITUD=@ID_SOLICITUD) OR EXISTS (SELECT 1 FROM dbo.FMI_SOLICITUD_APROBACION WHERE ID_SOLICITUD=@ID_SOLICITUD AND TIPO_SOLICITUD IN (''ASIGNACION_EQUIPO'',''CREDENCIALES''))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE,''CONFLICTO'' AS CODIGO_RESULTADO,N''La solicitud se gestiona por areas. Actualice la bandeja.'' AS MENSAJE;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_BANDEJA_ACCESO(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES))
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''No tiene permiso para visualizar la bandeja de aprobaciones.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        SELECT @ESTADO = S.ESTADO, @ID_CLIENTE = S.ID_CLIENTE
        FROM dbo.FMI_SOLICITUD_APROBACION S
        INNER JOIN dbo.FN_FMI_BANDEJA_CLIENTES_PERMITIDOS(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) C
            ON C.ID_CLIENTE = S.ID_CLIENTE
        WHERE S.ID_SOLICITUD = @ID_SOLICITUD
          AND EXISTS (SELECT 1 FROM dbo.FN_FMI_1124_SOLICITUDES_VISIBLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES) V
                      WHERE V.ID_SOLICITUD = S.ID_SOLICITUD)
          AND S.ID_ESTADO_REGISTRO = 1;

        IF @ESTADO IS NULL
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''NO_ENCONTRADO'' AS CODIGO_RESULTADO,
                   ''La solicitud no existe o no está asignada a sus clientes.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        IF NOT EXISTS (SELECT 1 FROM dbo.FN_FMI_APROBACION_CLIENTES_GESTIONABLES(@ID_USUARIO, @ID_EMPRESA, @ID_ROLES)
                       WHERE ID_CLIENTE = @ID_CLIENTE)
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''SIN_PERMISO'' AS CODIGO_RESULTADO,
                   ''Solo un gestor asignado al cliente puede editar esta solicitud.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        IF @DATOS_FORMULARIO IS NULL OR ISJSON(@DATOS_FORMULARIO) <> 1
        BEGIN
            SELECT 1 AS ID_TIPO_MENSAJE, ''PARAMETRO_INVALIDO'' AS CODIGO_RESULTADO,
                   ''Los datos del formulario no son válidos.'' AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        BEGIN TRANSACTION;

        UPDATE dbo.FMI_SOLICITUD_APROBACION
           SET DATOS_FORMULARIO = @DATOS_FORMULARIO,
               NOMBRE_TALENTO   = COALESCE(@NOMBRE_TALENTO, NOMBRE_TALENTO),
               USUMOD           = @USUARIO,
               FCHMOD           = GETDATE()
        OUTPUT CONVERT(BIGINT, INSERTED.VERSION_FILA) INTO @CAMBIADA (VERSION)
        WHERE ID_SOLICITUD = @ID_SOLICITUD
          AND ESTADO = ''PENDIENTE''
          AND CONVERT(BIGINT, VERSION_FILA) = @VERSION;

        IF NOT EXISTS (SELECT 1 FROM @CAMBIADA)
        BEGIN
            ROLLBACK TRANSACTION;
            SELECT 1 AS ID_TIPO_MENSAJE, ''CONFLICTO'' AS CODIGO_RESULTADO,
                   CASE WHEN @ESTADO <> ''PENDIENTE''
                        THEN ''La solicitud ya no está pendiente: no se puede editar.''
                        ELSE ''Otra persona modificó la solicitud. Recárguela antes de guardar.'' END AS MENSAJE,
                   CAST(NULL AS BIGINT) AS VERSION;
            RETURN;
        END;

        INSERT INTO dbo.FMI_SOLICITUD_APROBACION_HIST
            (ID_SOLICITUD, ACCION, ESTADO_ANTERIOR, ESTADO_NUEVO, DETALLE, ID_USUARIO, USUARIO, FCHCRE)
        VALUES
            (@ID_SOLICITUD, ''FORMULARIO_EDITADO'', ''PENDIENTE'', ''PENDIENTE'', @DETALLE, @ID_USUARIO, @USUARIO, GETDATE());

        COMMIT TRANSACTION;

        SELECT 2 AS ID_TIPO_MENSAJE, ''OK'' AS CODIGO_RESULTADO,
               ''Formulario guardado correctamente.'' AS MENSAJE,
               (SELECT VERSION FROM @CAMBIADA) AS VERSION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
        SELECT 3 AS ID_TIPO_MENSAJE, ''ERROR'' AS CODIGO_RESULTADO, ERROR_MESSAGE() AS MENSAJE,
               CAST(NULL AS BIGINT) AS VERSION;
    END CATCH;
END;
-- FTBO1124-alcance-v2';
COMMIT TRANSACTION;
SELECT DB_NAME() Base, N'FTBO-1124 areas instalado' Resultado;
