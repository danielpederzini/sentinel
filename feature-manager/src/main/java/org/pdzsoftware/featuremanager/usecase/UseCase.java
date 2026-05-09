package org.pdzsoftware.featuremanager.usecase;

public interface UseCase<I, O> {
    O execute(I input);
}
