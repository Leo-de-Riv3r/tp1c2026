package com.tacs.tp1c2026.entities;

import com.tacs.tp1c2026.entities.dto.output.AlertaDto;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.DocumentReference;


@Getter
public class MissingStickerAlert extends Alert {

    @DocumentReference
    private Sticker sticker;

    public MissingStickerAlert(Sticker sticker) {
        this.sticker = sticker;
    }

    @Override
    public AlertaDto visit(AlertVisitor visitor) {
        return visitor.visit(this);
    }

}
