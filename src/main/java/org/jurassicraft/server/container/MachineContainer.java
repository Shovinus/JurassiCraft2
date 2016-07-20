package org.jurassicraft.server.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class MachineContainer extends Container
{
    private int[] fields;
    private IInventory inventory;

    public MachineContainer(IInventory inventory)
    {
        this.inventory = inventory;
        this.fields = new int[inventory.getFieldCount()];
    }

    @Override
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for (IContainerListener listener : this.listeners)
        {
            for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++)
            {
                int field = inventory.getField(fieldIndex);

                if (field != fields[fieldIndex])
                {
                    listener.sendProgressBarUpdate(this, fieldIndex, field);
                }
            }
        }

        for (int fieldIndex = 0; fieldIndex < fields.length; fieldIndex++)
        {
            fields[fieldIndex] = inventory.getField(fieldIndex);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int data)
    {
        this.inventory.setField(id, data);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex)
    {
        ItemStack transferred = null;
        Slot slot = this.inventorySlots.get(slotIndex);

        int otherSlots = this.inventorySlots.size() - 36;

        if (slot != null && slot.getHasStack())
        {
            ItemStack current = slot.getStack();
            transferred = current.copy();

            if (slotIndex < otherSlots)
            {
                if (!this.mergeItemStack(current, otherSlots, this.inventorySlots.size(), true))
                {
                    return null;
                }
            }
            else if (!this.mergeItemStack(current, 0, otherSlots, false))
            {
                return null;
            }

            if (current.stackSize == 0)
            {
                slot.putStack(null);
            }
            else
            {
                slot.onSlotChanged();
            }
        }

        return transferred;
    }
}