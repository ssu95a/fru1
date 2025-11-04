package ru.inversion.fru.model.fields.functions;


import ru.inversion.fru.generator.FruContext;

import java.util.function.BiFunction;

/** */
public interface IFunctionImpl<P,V> extends BiFunction<FruContext, P, V > {
}
