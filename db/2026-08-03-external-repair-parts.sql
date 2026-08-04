-- ============================================================================
-- External repair part selection - schema migration
-- ============================================================================
-- Run this ONCE against each existing database (dev, staging, prod) BEFORE
-- deploying the external-repair part selection feature.
--
-- Why this is needed:
--   spring.jpa.hibernate.ddl-auto=update creates the two new nullable columns
--   automatically (external_repairs.part_catalog_id / part_quantity and
--   inventory_movements.external_repair_id), but it does NOT widen an existing
--   MySQL ENUM column. inventory_movements.movement_type was created as
--   enum('PURCHASE','SALE','REPAIR_USE','REPAIR_RETURN','ADJUSTMENT','DAMAGE'),
--   so inserting the two new movement types fails with
--   "ERROR 1265 Data truncated for column 'movement_type'".
--
-- On a brand-new database Hibernate generates the full enum and this script is
-- a no-op, but it is safe to run either way.
-- ============================================================================

ALTER TABLE inventory_movements
    MODIFY COLUMN movement_type ENUM(
        'PURCHASE',
        'SALE',
        'REPAIR_USE',
        'REPAIR_RETURN',
        'EXTERNAL_REPAIR_USE',
        'EXTERNAL_REPAIR_RETURN',
        'ADJUSTMENT',
        'DAMAGE'
    ) NOT NULL;
