package com.motorola.livestream.model.fb;

import com.motorola.livestream.R;

public class Reaction {
    private String id;
    private String name;
    private String type;
    private ReactionType reactionType;

    public Reaction() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ReactionType getType() {
        return reactionType;
    }

    public void setType(String type) {
        this.type = type;
        this.reactionType = ReactionType.valueOf(type);
    }

    public enum ReactionType {
        LIKE,
        LOVE,
        HAHA,
        WOW,
        SAD,
        ANGRY;

        private static final int[] ICON_MAP = new int[] {
                R.drawable.ic_live_reaction_like,
                R.drawable.ic_live_reaction_love,
                R.drawable.ic_live_reaction_haha,
                R.drawable.ic_live_reaction_wow,
                R.drawable.ic_live_reaction_sad,
                R.drawable.ic_live_reaction_angry,
        };

        public int getReactionIcon() {
            return ICON_MAP[ordinal()];
        }
    }
}
