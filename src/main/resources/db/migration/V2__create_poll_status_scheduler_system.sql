------------------------------------------------------------
-- Tabela de eventos de atualização de status
------------------------------------------------------------
CREATE TABLE status_to_update (
  id BIGSERIAL PRIMARY KEY,
  poll_id BIGINT NOT NULL REFERENCES poll (id) ON DELETE CASCADE,
  current_status VARCHAR(20) NOT NULL,
  next_status VARCHAR(20) NOT NULL,
  scheduled_date TIMESTAMP NOT NULL
);

------------------------------------------------------------
-- Índices recomendados
------------------------------------------------------------

-- Para buscar rapidamente o próximo evento pendente
CREATE INDEX idx_status_to_update_pending
    ON status_to_update (scheduled_date);

-- Para buscas por poll
CREATE INDEX idx_status_to_update_poll_id
    ON status_to_update (poll_id);

------------------------------------------------------------
-- Função auxiliar para criar eventos
------------------------------------------------------------
CREATE OR REPLACE FUNCTION create_status_update_event(
    p_id BIGINT,
    p_current_status VARCHAR,
    p_next_status VARCHAR,
    p_date TIMESTAMP
)
    RETURNS VOID AS $$
BEGIN
    INSERT INTO status_to_update (poll_id, current_status, next_status, scheduled_date)
    VALUES (p_id, p_current_status, p_next_status, p_date);

    PERFORM pg_notify(
            'status_to_update_channel',
--             (p_id::text || '|' || to_char(p_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS'))::text
            (p_id::text || '|' || to_char(p_date, 'YYYY-MM-DD"T"HH24:MI:SS'))::text
            );
END;
$$ LANGUAGE plpgsql;

------------------------------------------------------------
-- Função do trigger principal
------------------------------------------------------------
CREATE OR REPLACE FUNCTION schedule_poll_status_update()
    RETURNS TRIGGER AS $$

DECLARE
    old_start TIMESTAMP;
    old_end   TIMESTAMP;

BEGIN
    --------------------------------------------------------------------
    -- Caso DELETE: remova todos os eventos pendentes e encerre
    --------------------------------------------------------------------
    IF TG_OP = 'DELETE' THEN
        DELETE FROM status_to_update WHERE poll_id = OLD.id;
        RETURN OLD;
    END IF;

    --------------------------------------------------------------------
    -- Caso INSERT: criar os dois eventos (START e END), se aplicável
    --------------------------------------------------------------------
    IF TG_OP = 'INSERT' THEN
        -- Se já começar como FINISHED, não cria nada
        IF NEW.status <> 'FINISHED' THEN

            -- Evento para START (se ainda não começou)
            IF NEW.status = 'NOT_STARTED' THEN
                PERFORM create_status_update_event(
                        NEW.id,
                        'NOT_STARTED',
                        'STARTED',
                        NEW.start_date
                        );
            END IF;

            -- Evento para FINISHED
            PERFORM create_status_update_event(
                    NEW.id,
                    'STARTED',
                    'FINISHED',
                    NEW.end_date
                    );
        END IF;

        RETURN NEW;
    END IF;

    --------------------------------------------------------------------
    -- Caso UPDATE: criar eventos apenas se datas mudarem
    --------------------------------------------------------------------
    IF TG_OP = 'UPDATE' THEN

        old_start := OLD.start_date;
        old_end   := OLD.end_date;

        ----------------------------------------------------------------
        -- Se datas mudaram, recriar eventos
        ----------------------------------------------------------------
        IF old_start <> NEW.start_date OR old_end <> NEW.end_date THEN

            -- remove eventos pendentes antigos
            DELETE FROM status_to_update
            WHERE poll_id = NEW.id;

            IF NEW.status <> 'FINISHED' THEN

                -- evento START (apenas se ainda não começou)
                IF NEW.start_date > NOW() THEN
                    PERFORM create_status_update_event(
                            NEW.id,
                            'NOT_STARTED',
                            'STARTED',
                            NEW.start_date
                            );
                END IF;

                -- evento FINISHED (sempre que end_date for futura)
                IF NEW.end_date > NOW() THEN
                    PERFORM create_status_update_event(
                            NEW.id,
                            'STARTED',
                            'FINISHED',
                            NEW.end_date
                            );
                END IF;

            END IF;
        END IF;

        RETURN NEW;
    END IF;

    --------------------------------------------------------------------
    -- fallback
    --------------------------------------------------------------------
    RETURN NEW;

END;
$$ LANGUAGE plpgsql;

------------------------------------------------------------
-- Trigger que dispara para INSERT, UPDATE e DELETE
------------------------------------------------------------
CREATE OR REPLACE TRIGGER trigger_schedule_poll_status_update
    AFTER
        INSERT OR
        UPDATE OF start_date, end_date
        ON poll
    FOR EACH ROW
EXECUTE FUNCTION schedule_poll_status_update();
