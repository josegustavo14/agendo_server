-- Adiciona coluna de status ao appointments
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- Preenche registros existentes com PENDING
UPDATE appointments SET status = 'PENDING' WHERE status IS NULL;

-- Aplica constraint NOT NULL após preencher os dados
ALTER TABLE appointments ALTER COLUMN status SET NOT NULL;
