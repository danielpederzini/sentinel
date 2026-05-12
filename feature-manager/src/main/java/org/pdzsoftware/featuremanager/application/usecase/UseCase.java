package org.pdzsoftware.featuremanager.application.usecase;

public interface UseCase<I, O> {
    O execute(I input);
}
