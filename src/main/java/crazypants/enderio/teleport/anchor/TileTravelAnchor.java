package crazypants.enderio.teleport.anchor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.TileEntityEio;
import crazypants.enderio.api.teleport.ITravelAccessable;
import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.machine.painter.IPaintableTileEntity;
import crazypants.util.PlayerUtil;

public class TileTravelAnchor extends TileEntityEio implements ITravelAccessable, IPaintableTileEntity {

  private static final String KEY_SOURCE_BLOCK_ID = "sourceBlock";
  private static final String KEY_SOURCE_BLOCK_META = "sourceBlockMeta";
  
  private Block sourceBlock;
  private int sourceBlockMetadata;

  private AccessMode accessMode = AccessMode.PUBLIC;

  private ItemStack[] password = new ItemStack[5];
  
  private ItemStack itemLabel;
  
  private String label;

  private UUID placedBy;

  private List<UUID> authorisedUsers = new ArrayList<UUID>();

  @Override
  public boolean canBlockBeAccessed(EntityPlayer playerName) {
    if(accessMode == AccessMode.PUBLIC) {
      return true;
    }
    if(accessMode == AccessMode.PRIVATE) {
      return placedBy != null && placedBy.equals(PlayerUtil.getPlayerUUID(playerName.getGameProfile().getName()));
    }
    if(placedBy != null && placedBy.equals(PlayerUtil.getPlayerUUID(playerName.getGameProfile().getName()))) {
      return true;
    }
    return authorisedUsers.contains(PlayerUtil.getPlayerUUID(playerName.getGameProfile().getName()));
  }

  @Override
  public void clearAuthorisedUsers() {
    authorisedUsers.clear();
  }

  private boolean checkPassword(ItemStack[] pwd) {
    if(pwd == null || pwd.length != password.length) {
      return false;
    }
    for (int i = 0; i < pwd.length; i++) {
      ItemStack pw = password[i];
      ItemStack tst = pwd[i];
      if(pw == null && tst != null) {
        return false;
      }
      if(pw != null) {
        if(tst == null || !ItemStack.areItemStacksEqual(pw, tst)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public boolean getRequiresPassword(EntityPlayer username) {
    return getAccessMode() != AccessMode.PUBLIC && !canUiBeAccessed(username) && !authorisedUsers.contains(PlayerUtil.getPlayerUUID(username.getGameProfile().getName()));
  }

  @Override
  public boolean authoriseUser(EntityPlayer username, ItemStack[] password) {
    if(checkPassword(password)) {
      authorisedUsers.add(PlayerUtil.getPlayerUUID(username.getGameProfile().getName()));
      return true;
    }
    return false;
  }

  @Override
  public boolean canUiBeAccessed(EntityPlayer playerName) {
    return placedBy != null && placedBy.equals(PlayerUtil.getPlayerUUID(playerName.getGameProfile().getName()));
  }

  @Override
  public boolean canSeeBlock(EntityPlayer playerName) {
    if(accessMode != AccessMode.PRIVATE) {
      return true;
    }
    return placedBy != null && placedBy.equals(PlayerUtil.getPlayerUUID(playerName.getGameProfile().getName()));
  }

  @Override
  public AccessMode getAccessMode() {
    return accessMode;
  }

  @Override
  public void setAccessMode(AccessMode accessMode) {
    this.accessMode = accessMode;
  }

  @Override
  public ItemStack[] getPassword() {
    return password;
  }

  @Override
  public void setPassword(ItemStack[] password) {
    this.password = password;
  }

  public ItemStack getItemLabel() {
    return itemLabel;
  }

  public void setItemLabel(ItemStack lableIcon) {
    this.itemLabel = lableIcon;
  }

  @Override
  public String getLabel() {  
    return label;
  }

  @Override
  public void setLabel(String label) {
    this.label = label;    
  }

  @Override
  public UUID getPlacedBy() {
    return placedBy;
  }

  @Override
  public void setPlacedBy(EntityPlayer player) {
    if(player == null || player.getGameProfile() == null) {
      this.placedBy = null;
    } else {
      placedBy = PlayerUtil.getPlayerUUID(player.getGameProfile().getName());
    }
  }

  @Override
  public boolean shouldUpdate() {
    return false;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public double getMaxRenderDistanceSquared() {
    return TravelSource.getMaxDistanceSq();
  }

  @Override
  public boolean shouldRenderInPass(int pass) {
    return pass == 1;
  }
  
  @Override
  public Block getSourceBlock() {
    return sourceBlock;
  }

  @Override
  public void setSourceBlock(Block sourceBlock) {
    this.sourceBlock = sourceBlock;
  }

  @Override
  public int getSourceBlockMetadata() {
    return sourceBlockMetadata;
  }

  @Override
  public void setSourceBlockMetadata(int sourceBlockMetadata) {
    this.sourceBlockMetadata = sourceBlockMetadata;
  }

  @Override
  protected void readCustomNBT(NBTTagCompound root) {
    if(root.hasKey("accessMode")) {
      accessMode = AccessMode.values()[root.getShort("accessMode")];
    } else {
      //keep behavior the same for blocks placed prior to this update
      accessMode = AccessMode.PUBLIC;
    }
    placedBy = PlayerUtil.getPlayerUIDUnstable(root.getString("placedBy"));
    for (int i = 0; i < password.length; i++) {
      if(root.hasKey("password" + i)) {
        NBTTagCompound stackRoot = (NBTTagCompound) root.getTag("password" + i);
        password[i] = ItemStack.loadItemStackFromNBT(stackRoot);
      } else {
        password[i] = null;
      }
    }
    authorisedUsers.clear();
    String userStr = root.getString("authorisedUsers");
    if(userStr != null && userStr.length() > 0) {
      String[] users = userStr.split(",");
      for (String user : users) {
        if(user != null) {
          user = user.trim();
          if(user.length() > 0) {
            authorisedUsers.add(PlayerUtil.getPlayerUIDUnstable(user));
          }
        }
      }
    }
    if(root.hasKey("itemLabel")) {
      NBTTagCompound stackRoot = (NBTTagCompound) root.getTag("itemLabel");
      itemLabel = ItemStack.loadItemStackFromNBT(stackRoot);
    } else {
      itemLabel = null;
    }
    
    String sourceBlockStr = root.getString(KEY_SOURCE_BLOCK_ID);
    sourceBlock = Block.getBlockFromName(sourceBlockStr);
    sourceBlockMetadata = root.getInteger(KEY_SOURCE_BLOCK_META);
    
    label = root.getString("label");
    if(label == null || label.trim().length() == 0) {
      label = null;
    }    
  }

  @Override
  protected void writeCustomNBT(NBTTagCompound root) {
    root.setShort("accessMode", (short) accessMode.ordinal());
    if(placedBy != null ) {
      root.setString("placedBy", placedBy.toString());
    }
    for (int i = 0; i < password.length; i++) {
      ItemStack stack = password[i];
      if(stack != null) {
        NBTTagCompound stackRoot = new NBTTagCompound();
        stack.writeToNBT(stackRoot);
        root.setTag("password" + i, stackRoot);
      }
    }
    StringBuffer userStr = new StringBuffer();
    for (UUID user : authorisedUsers) {
      if(user != null) {
        userStr.append(user.toString());
        userStr.append(",");
      }
    }
    if(authorisedUsers.size() > 0) {
      root.setString("authorisedUsers", userStr.toString());
    }
    if(itemLabel != null) {
      NBTTagCompound labelRoot = new NBTTagCompound();
      itemLabel.writeToNBT(labelRoot);
      root.setTag("itemLabel", labelRoot);
    }
    
    if(sourceBlock != null) {
      root.setString(KEY_SOURCE_BLOCK_ID, Block.blockRegistry.getNameForObject(sourceBlock));
    }
    root.setInteger(KEY_SOURCE_BLOCK_META, sourceBlockMetadata);
    
    if(label != null && label.trim().length() > 0) {
      root.setString("label", label);
    }
    
  }
  
  @Override
  public Packet getDescriptionPacket() {
    NBTTagCompound tag = new NBTTagCompound();
    writeCustomNBT(tag);
    return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag);
  }

  @Override
  public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
    readCustomNBT(pkt.func_148857_g());
  }
}
