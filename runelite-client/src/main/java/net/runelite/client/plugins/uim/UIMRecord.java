package net.runelite.client.plugins.uim;

import lombok.Value;

@Value
class UIMRecord {
    private final String title;
    private final String subTitle;
    private final UIMItem[] items;
    private final long timestamp;

    /**
     * Checks if this record matches specified id
     * @param id other record id
     * @return true if match is made
     */
    boolean matches(final String id)
    {
        if (id == null)
        {
            return true;
        }

        return title.equals(id);
    }
}
