package org.jurassicraft.server.entity.vehicle;


import net.minecraft.world.World;
import org.jurassicraft.server.item.ItemHandler;

public class FordExplorerEntity extends CarEntity {

    public FordExplorerEntity(World world) {
        super(world);
    }

    @Override
    public void dropItems() {
        this.dropItem(ItemHandler.FORD_EXPLORER, 1);
    }

    @Override
    protected Seat[] createSeats() {
        Seat frontLeft = new Seat(0, 0.563F, 0.45F, 0.4F, 0.5F, 0.25F);
        Seat frontRight = new Seat(1, -0.563F, 0.45F, 0.4F, 0.5F, 0.25F);
        Seat backLeft = new Seat(2, 0.5F, 0.7F, -2.2F, 0.4F, 0.25F);
        Seat backRight = new Seat(3, -0.5F, 0.7F, -2.2F, 0.4F, 0.25F);
        return new Seat[] { frontLeft, frontRight, backLeft, backRight };
    }

    @Override
    protected WheelData createWheels() {
	return new WheelData(1.3, 2, -1.3, -2.2);
    }
}
