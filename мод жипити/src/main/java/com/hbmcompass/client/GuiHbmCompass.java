package com.hbmcompass.client;

import com.hbmcompass.HbmCompassMod;
import com.hbmcompass.network.PacketSearchStructure;
import com.hbmcompass.network.PacketSyncResult;
import com.hbmcompass.network.PacketTeleport;
import com.hbmcompass.scanner.StructureRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuiHbmCompass extends GuiScreen {

    private final ItemStack compassStack;
    private GuiTextField searchField;
    private GuiButton btnSearch;
    private GuiButton btnTeleport;

    private final List<StructureRegistry.StructureEntry> allEntries = new ArrayList<>();
    private final List<StructureRegistry.StructureEntry> filteredEntries = new ArrayList<>();

    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 9;

    private String statusText = "Оберіть структуру зі списку та натисніть «Знайти»";
    private boolean hasFoundCoords = false;
    private int foundX = 0, foundY = 70, foundZ = 0;

    public GuiHbmCompass(ItemStack compassStack) {
        this.compassStack = compassStack;
        this.allEntries.addAll(StructureRegistry.getAllStructures());
        this.filteredEntries.addAll(this.allEntries);

        NBTTagCompound tag = compassStack.getTagCompound();
        if (tag != null && tag.hasKey("StructureId")) {
            String savedId = tag.getString("StructureId");
            for (int i = 0; i < filteredEntries.size(); i++) {
                if (filteredEntries.get(i).id.equals(savedId)) {
                    selectedIndex = i;
                    break;
                }
            }
            if (tag.getBoolean("Found")) {
                hasFoundCoords = true;
                foundX = tag.getInteger("TargetX");
                foundY = tag.getInteger("TargetY");
                foundZ = tag.getInteger("TargetZ");
                statusText = "Активна ціль: X=" + foundX + ", Z=" + foundZ;
            }
        }
    }

    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int topY = this.height / 2 - 110;

        this.searchField = new GuiTextField(0, this.fontRenderer, centerX - 140, topY + 22, 280, 16);
        this.searchField.setMaxStringLength(50);
        this.searchField.setFocused(true);

        this.buttonList.clear();
        this.btnSearch = new GuiButton(1, centerX - 140, topY + 190, 136, 20, "Знайти найближчу");
        this.btnTeleport = new GuiButton(2, centerX + 4, topY + 190, 136, 20, "Телепорт [X/Z]");
        this.btnTeleport.enabled = this.hasFoundCoords;

        this.buttonList.add(this.btnSearch);
        this.buttonList.add(this.btnTeleport);
    }

    private void updateFilter() {
        String q = this.searchField.getText().trim().toLowerCase(Locale.ROOT);
        this.filteredEntries.clear();
        for (StructureRegistry.StructureEntry entry : this.allEntries) {
            if (q.isEmpty() || entry.displayName.toLowerCase(Locale.ROOT).contains(q) || entry.id.toLowerCase(Locale.ROOT).contains(q)) {
                this.filteredEntries.add(entry);
            }
        }
        this.selectedIndex = 0;
        this.scrollOffset = 0;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1 && !filteredEntries.isEmpty() && selectedIndex >= 0 && selectedIndex < filteredEntries.size()) {
            StructureRegistry.StructureEntry chosen = filteredEntries.get(selectedIndex);
            this.statusText = "Сканування світу для: " + chosen.displayName + "...";
            HbmCompassMod.NETWORK.sendToServer(new PacketSearchStructure(chosen.id, 300));
        } else if (button.id == 2 && this.hasFoundCoords) {
            HbmCompassMod.NETWORK.sendToServer(new PacketTeleport(this.foundX, this.foundY, this.foundZ));
            this.mc.displayGuiScreen(null);
        }
    }

    public void onSearchResult(PacketSyncResult msg) {
        this.hasFoundCoords = msg.found;
        this.foundX = msg.x;
        this.foundY = msg.y;
        this.foundZ = msg.z;
        this.btnTeleport.enabled = msg.found;
        if (msg.found) {
            this.statusText = "Знайдено! Координати: X=" + msg.x + ", Z=" + msg.z;
        } else {
            this.statusText = "Не знайдено в радіусі 4800 блоків.";
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int maxScroll = Math.max(0, filteredEntries.size() - VISIBLE_ROWS);
            if (wheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.searchField.mouseClicked(mouseX, mouseY, mouseButton);

        int centerX = this.width / 2;
        int topY = this.height / 2 - 110;
        int listX = centerX - 140;
        int listY = topY + 44;
        int rowHeight = 15;

        if (mouseX >= listX && mouseX <= listX + 280 && mouseY >= listY && mouseY <= listY + VISIBLE_ROWS * rowHeight) {
            int clickedRow = (mouseY - listY) / rowHeight;
            int idx = scrollOffset + clickedRow;
            if (idx >= 0 && idx < filteredEntries.size()) {
                selectedIndex = idx;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.searchField.textboxKeyTyped(typedChar, keyCode)) {
            updateFilter();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int centerX = this.width / 2;
        int topY = this.height / 2 - 110;

        // Темна панель у стилі приладів HBM
        drawRect(centerX - 150, topY, centerX + 150, topY + 220, 0xDD141814);
        drawRect(centerX - 148, topY + 2, centerX + 148, topY + 218, 0xFF222A22);

        this.drawCenteredString(this.fontRenderer, "§a§lHBM NTM Explorer's Compass (1.12.2)", centerX, topY + 8, 0xFFFFFF);
        this.searchField.drawTextBox();

        int listX = centerX - 140;
        int listY = topY + 44;
        int rowHeight = 15;

        drawRect(listX, listY, listX + 280, listY + VISIBLE_ROWS * rowHeight, 0xFF101210);

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int idx = scrollOffset + i;
            if (idx >= filteredEntries.size()) break;

            StructureRegistry.StructureEntry entry = filteredEntries.get(idx);
            int rowY = listY + i * rowHeight;

            if (idx == selectedIndex) {
                drawRect(listX + 1, rowY, listX + 279, rowY + rowHeight, 0xFF2E5B2E);
            } else if (mouseX >= listX && mouseX <= listX + 280 && mouseY >= rowY && mouseY < rowY + rowHeight) {
                drawRect(listX + 1, rowY, listX + 279, rowY + rowHeight, 0xFF1E2E1E);
            }

            this.fontRenderer.drawStringWithShadow(entry.displayName, listX + 6, rowY + 3, 0xE0FFE0);
        }

        this.drawCenteredString(this.fontRenderer, this.statusText, centerX, topY + 180, 0xFFD700);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
