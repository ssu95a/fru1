package ru.inversion.fru.generator.renderer;

import ru.inversion.fru.generator.FruContext;
import ru.inversion.fru.model.items.FruItem;

@FunctionalInterface
public interface IRenderer<T extends FruItem> {
    // Рендерит элемент в контексте генерации отчета
    void render( FruContext context, T item );
}
