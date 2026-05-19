package com.oppshan.securedoc.common;

import jakarta.annotation.Nonnull;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.StaleStateException;
import org.hibernate.exception.ConstraintViolationException;

import static java.util.Objects.requireNonNull;

/**
 * Mix-in for Jakarta Data repositories so they can mutate entities in the
 * currently active JPA session. Hibernate ORM 7 generates Jakarta Data
 * impls on top of a {@code StatelessSession} which does not dirty-track
 * managed entities -- so the conventional pattern of {@code findById} ->
 * mutate -> {@code save()} silently no-ops on the mutation. Calling
 * {@link #updateWithSession(Object)} grabs the request-scoped
 * {@code EntityManager} via CDI and merges through it instead.
 */
public interface StatefulWriteRepository<T> {

    default <S extends T> S updateWithSession(@Nonnull S entity) {
        requireNonNull(entity, "Null entity");

        try {
            CDI.current().select(EntityManager.class).get().merge(entity);
        } catch (StaleStateException ex) {
            throw new OptimisticLockingFailureException(ex.getMessage(), ex);
        } catch (PersistenceException ex) {
            throw new DataException(ex.getMessage(), ex);
        }

        return entity;
    }

    default <S extends T> S saveWithSession(@Nonnull S entity) {
        try {
            return updateWithSession(entity);
        } catch (Exception ex) {
            return insertWithSession(entity);
        }
    }

    default <S extends T> void deleteWithSession(@Nonnull S entity) {
        requireNonNull(entity, "Null entity");

        try {
            CDI.current().select(EntityManager.class).get().remove(entity);
        } catch (StaleStateException ex) {
            throw new OptimisticLockingFailureException(ex.getMessage(), ex);
        } catch (PersistenceException ex) {
            throw new DataException(ex.getMessage(), ex);
        }
    }

    default <S extends T> S insertWithSession(@Nonnull S entity) {
        requireNonNull(entity, "Null entity");

        try {
            CDI.current().select(EntityManager.class).get().persist(entity);
        } catch (ConstraintViolationException ex) {
            throw new EntityExistsException(ex.getMessage(), ex);
        } catch (PersistenceException ex) {
            throw new DataException(ex.getMessage(), ex);
        }

        return entity;
    }

    default <S extends T> S attachWithSession(@Nonnull S entity) {
        requireNonNull(entity, "Null entity");

        return CDI.current().select(EntityManager.class).get().merge(entity);
    }

    default <S extends T> S refreshWithSession(@Nonnull S entity) {
        requireNonNull(entity, "Null entity");

        CDI.current().select(EntityManager.class).get().refresh(entity);
        return entity;
    }

    default void flushWithSession() {
        CDI.current().select(EntityManager.class).get().flush();
    }
}
