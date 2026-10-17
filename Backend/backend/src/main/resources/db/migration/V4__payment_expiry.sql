-- Kapan Snap token kedaluwarsa — supaya bisa dipakai ulang selama masih berlaku
ALTER TABLE payments ADD (expires_at TIMESTAMP(6) WITH TIME ZONE);