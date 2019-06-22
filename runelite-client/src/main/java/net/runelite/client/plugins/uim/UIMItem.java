package net.runelite.client.plugins.uim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
class UIMItem
{
    @Getter
    private final int id;
    @Getter
    private final String name;
    @Getter
    private final int quantity;
    @Getter
    private final boolean isStackable;
    @Getter
    @Setter
    private boolean ignored;
}
