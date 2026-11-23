package com.meshi.fairy.client;

import com.meshi.fairy.FairyEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

// quiet vanilla bee loop that follows the fairy
public class BuzzSound extends AbstractTickableSoundInstance {
    private final FairyEntity fairy;

    public BuzzSound(FairyEntity fairy) {
        super(FairyEntity.sfx("entity.bee.loop"), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.fairy = fairy;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.1f;
        this.pitch = 1.0f;
        this.x = fairy.getX();
        this.y = fairy.getY();
        this.z = fairy.getZ();
    }

    @Override
    public void tick() {
        if (fairy.isRemoved()) {
            stop();
            return;
        }
        this.x = fairy.getX();
        this.y = fairy.getY();
        this.z = fairy.getZ();
    }
}
