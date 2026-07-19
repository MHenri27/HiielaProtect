# HiielaProtect - plugin for MC Hiiela

Make managing WorldGuard regions easy, fast and actually manageable for staff members. Create simple protections for players using few commands. Track everything and enjoy this decent yet simple wrapper.

---

- 🔢 **Automatic indexing:** Automatically scales the index indicator of regions depending on how many the player has.
- 📊 **Stats & tracking:** Tracks everything and offers a simple leaderboard for staff members. 
- 💾 **SQLite:** Saves all data into local SQLite.

---

## 📦 Dependencies
- **MC server 26.1.2 (native)**
- **WorldEdit (latest)**
- **WorldGuard (latest)**
- **PlaceholderAPI (latest) [soft]**

---

## Details

### Through internal APIs
Rather than literally wrapping other commands into one command like a alias, I register the regions straight into WG registries and for all kinds of different checks I use WE & WG calls. Prevents overlapping, supports multiworld, prevents little mistakes with selections etc.

### Comfortable
Players get short, easy to use commands to manage members and owners of their regions. Really good region specifying inside the commands, either use region id, your username, region number or just simply stand in your region. Same goes for admin commands, really comfortable to use.

### Modern design
Solid tab completion, 100% configurability etc.

---

## 📜 Commands and Syntax

Base command is "/kaitse".

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/kaitse loo <player> [yes/no]` | `admin` | Creates a new region for player based on your WE selection, prevents all kinds of mistakes, specify commands to run before & after the creation in config. Choose if you want to expand vertically or not.|
| `/kaitse kustuta <@here \| #region \| player>`| `admin` | Deletes a region. You can delete the region you're currently standing in (`@here`), a specific region by its name (`#Henri_1`), or the last region created for a player by specifying the player's name. |
| `/kaitse liiguta <#region \| player> [yes/no]` | `admin` | Moves an existing region to a new location (using your current WorldEdit selection). All owners, members, and flags are preserved. |
| `/kaitse info [player]` | `player` / `admin` | Displays the number of regions a player owns and a list of those regions. You can also view another player's regions by specifying the player's name as an argument. |
| `/kaitse statistika` | `admin` | Displays the total number of regions you have created as an administrator, as well as the server's TOP 3 staff members who have created the most regions throughout the server's history. |
| `/kaitse lisaomanik <player> [#reg. \| number]` | `player` / `admin` | Adds a new owner to a region. If a region name is not specified, the region the player is currently standing in will be used. |
| `/kaitse eemaldaomanik <player> [#reg. \| number]`| `player` / `admin` | Removes specified owner from a region. (Cannot remove yourself). |
| `/kaitse lisaliige <player> [#reg. \| number]` | `player` / `admin` | Adds a new member to a region. |
| `/kaitse eemaldaliige <player> [#reg. \| number]`| `player` / `admin` | Removes a specified member from a region. |
| `/kaitse taaslae` | `admin` | Reloads the config. |

*Players can use user modify commands only in regions they are owners in.*

---

## Setup

1. Upload all dependencies & HiielaProtect
2. Restart
3. Configure
4. Enjoy
