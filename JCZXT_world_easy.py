import json
import random
import tkinter as tk
from collections import deque
from pathlib import Path
from tkinter import messagebox


DATA_FILE = Path(__file__).with_name("JCZXT_M.json")
COLS = 60
ROWS = 20
CELL = 16
BANK_CELLS = {(x, y) for x in range(1, 5) for y in range(1, 5)}
J_CELLS = {(x, y) for x in range(6, 8) for y in range(6, 8)}
T_CELLS = {(x, y) for x in range(59, 61) for y in range(19, 21)}
KEY_AREA = {(x, y) for x in range(25, 36) for y in range(5, 16)}
DIRECTIONS = ((0, -1), (0, 1), (-1, 0), (1, 0))


class EasyWorldApp:
    def __init__(self, root):
        self.root = root
        self.root.title("JCZXT 简单世界")
        self.root.geometry("1100x620")
        self.root.minsize(760, 500)
        self.root.configure(bg="#f3f5f7")
        self.game_started = False
        self.game_over = False
        self.turn = "T"
        self.round_number = 1
        self.player_role = None
        self.player = None
        self.robot = None
        self.has_key = False
        self.robot_has_key = False
        self.obstacles = set()
        self.secondary_obstacles = set()
        self.key = None
        self.cell_ids = {}

        header = tk.Frame(root, bg="#202b36")
        header.pack(fill="x")
        tk.Label(header, text="JCZXT简单世界", bg="#202b36", fg="white", font=("Microsoft YaHei UI", 20, "bold")).pack(side="left", padx=24, pady=14)
        self.status = tk.Label(header, text="请选择角色", bg="#202b36", fg="#ffd166", font=("Microsoft YaHei UI", 11))
        self.status.pack(side="right", padx=24)

        self.choice_bar = tk.Frame(root, bg="#f3f5f7")
        self.choice_bar.pack(fill="x", padx=20, pady=(14, 4))
        tk.Label(self.choice_bar, text="选择出生位置：", bg="#f3f5f7", fg="#202b36", font=("Microsoft YaHei UI", 11, "bold")).pack(side="left")
        self.choice_buttons = {}
        for role in ("T", "J"):
            button = tk.Button(self.choice_bar, text=f"作为 {role} 出生", command=lambda value=role: self.start_game(value), relief="flat", bd=0, bg="#2a9d8f", fg="white", activebackground="#21867b", padx=14, pady=6)
            button.pack(side="left", padx=6)
            self.choice_buttons[role] = button

        map_area = tk.Frame(root, bg="#f3f5f7")
        map_area.pack(fill="both", expand=True, padx=20, pady=12)
        self.canvas = tk.Canvas(map_area, bg="white", highlightthickness=0, width=960, height=640)
        horizontal = tk.Scrollbar(map_area, orient="horizontal", command=self.canvas.xview)
        vertical = tk.Scrollbar(map_area, orient="vertical", command=self.canvas.yview)
        self.canvas.configure(xscrollcommand=horizontal.set, yscrollcommand=vertical.set)
        self.canvas.grid(row=0, column=0, sticky="nsew")
        vertical.grid(row=0, column=1, sticky="ns")
        horizontal.grid(row=1, column=0, sticky="ew")
        map_area.grid_rowconfigure(0, weight=1)
        map_area.grid_columnconfigure(0, weight=1)
        self.canvas.bind("<Configure>", lambda event: self.canvas.configure(scrollregion=self.canvas.bbox("all")))
        self.canvas.bind_all("<KeyPress>", self.handle_key)

        self.loading_frame = tk.Frame(root, bg="#0288D1")
        self.loading_frame.place(x=0, y=0, relwidth=1, relheight=1)
        tk.Label(
            self.loading_frame,
            text="JCZXT简单世界正在加载中",
            bg="#0288D1",
            fg="white",
            font=("Microsoft YaHei UI", 24, "bold"),
        ).place(relx=0.5, rely=0.5, anchor="center")
        root.update_idletasks()
        root.update()
        self.build_world()
        self.loading_frame.destroy()

    def build_world(self):
        protected = BANK_CELLS | J_CELLS | T_CELLS
        self.obstacles = set()
        for y in range(1, ROWS + 1):
            for x in range(1, COLS + 1):
                if (x, y) not in protected and random.randrange(20) == 0:
                    self.obstacles.add((x, y))

        self.secondary_obstacles = set()
        for cell in tuple(self.obstacles):
            outcome = random.randrange(6)
            count = (4 if outcome == 0 else 0 if outcome == 1 else 2 if outcome in (2, 3) else 3)
            neighbors = [neighbor for neighbor in self.neighbors(cell) if neighbor not in protected and neighbor not in self.obstacles and neighbor not in self.secondary_obstacles]
            random.shuffle(neighbors)
            self.secondary_obstacles.update(neighbors[:count])

        t_start = self.random_cell(T_CELLS)
        self.carve_path(t_start, self.random_cell(BANK_CELLS))
        self.key = self.find_key(t_start)
        self.carve_path(t_start, self.key)
        self.ensure_key_obstacles(t_start)
        self.draw_world()

    def find_key(self, start):
        candidates = list(KEY_AREA)
        random.shuffle(candidates)
        for candidate in candidates:
            obstacle_count = sum(neighbor in self.obstacles for neighbor in self.neighbors(candidate))
            if obstacle_count >= 2 and self.has_path(start, candidate):
                return candidate
        candidate = random.choice(candidates)
        self.carve_path(start, candidate)
        for neighbor in self.neighbors(candidate)[:2]:
            self.obstacles.add(neighbor)
        return candidate

    def has_path(self, start, goal):
        pending = deque([start])
        visited = {start}
        while pending:
            current = pending.popleft()
            if current == goal:
                return True
            for neighbor in self.neighbors(current):
                if neighbor not in visited and neighbor not in self.obstacles and neighbor not in self.secondary_obstacles:
                    visited.add(neighbor)
                    pending.append(neighbor)
        return False

    def ensure_key_obstacles(self, start):
        obstacle_count = sum(neighbor in self.obstacles for neighbor in self.neighbors(self.key))
        candidates = [
            neighbor for neighbor in self.neighbors(self.key)
            if neighbor not in self.obstacles
            and neighbor not in self.secondary_obstacles
            and neighbor not in BANK_CELLS | J_CELLS | T_CELLS
        ]
        random.shuffle(candidates)
        for candidate in candidates:
            if obstacle_count >= 2:
                break
            self.obstacles.add(candidate)
            if self.has_path(start, self.key):
                obstacle_count += 1
            else:
                self.obstacles.remove(candidate)

    def random_cell(self, cells):
        return random.choice(tuple(cells))

    def neighbors(self, cell):
        x, y = cell
        return [(x + dx, y + dy) for dx, dy in DIRECTIONS if 1 <= x + dx <= COLS and 1 <= y + dy <= ROWS]

    def carve_path(self, start, goal):
        current = start
        while current != goal:
            x, y = current
            gx, gy = goal
            if x != gx and (random.randrange(2) or y == gy):
                current = (x + (1 if gx > x else -1), y)
            else:
                current = (x, y + (1 if gy > y else -1))
            self.obstacles.discard(current)
            self.secondary_obstacles.discard(current)

    def draw_world(self):
        self.canvas.delete("all")
        self.cell_ids = {}
        protected_groups = ((BANK_CELLS, "BANK"), (J_CELLS, "J"), (T_CELLS, "T"))
        for y in range(1, ROWS + 1):
            for x in range(1, COLS + 1):
                cell = (x, y)
                left = (x - 1) * CELL
                top = (y - 1) * CELL
                fill = "#101010" if cell in self.obstacles or cell in self.secondary_obstacles else "#ffffff"
                self.cell_ids[cell] = self.canvas.create_rectangle(left, top, left + CELL, top + CELL, fill=fill, outline="#d9dfe3")
        for cells, label in protected_groups:
            min_x = min(x for x, _ in cells)
            min_y = min(y for _, y in cells)
            max_x = max(x for x, _ in cells)
            max_y = max(y for _, y in cells)
            self.canvas.create_rectangle((min_x - 1) * CELL, (min_y - 1) * CELL, max_x * CELL, max_y * CELL, outline="#000000", width=2)
            self.canvas.create_text(((min_x + max_x - 1) * CELL) / 2, ((min_y + max_y - 1) * CELL) / 2, text=label, fill="#111111", font=("Microsoft YaHei UI", 10, "bold"))
        if self.key:
            x, y = self.key
            self.canvas.create_text((x - 0.5) * CELL, (y - 0.5) * CELL, text="🔑", font=("Segoe UI Emoji", 11))
        self.draw_characters()
        self.canvas.configure(scrollregion=(0, 0, COLS * CELL, ROWS * CELL))

    def draw_characters(self):
        for role, position, color in (("T", self.player if self.player_role == "T" else self.robot, "#e76f51"), ("J", self.player if self.player_role == "J" else self.robot, "#457b9d")):
            if position is not None:
                x, y = position
                self.canvas.create_text((x - 0.5) * CELL, (y - 0.5) * CELL, text=role, fill=color, font=("Microsoft YaHei UI", 11, "bold"))

    def start_game(self, role):
        self.player_role = role
        self.player = self.random_cell(T_CELLS if role == "T" else J_CELLS)
        self.robot = self.random_cell(J_CELLS if role == "T" else T_CELLS)
        self.game_started = True
        self.game_over = False
        self.turn = "T"
        self.round_number = 1
        self.has_key = False
        self.robot_has_key = False
        self.update_status()
        self.draw_world()
        self.canvas.focus_set()
        if self.player_role == "J":
            self.robot_move()
            self.turn = "J"
            self.draw_world()
            self.update_status()

    def handle_key(self, event):
        if not self.game_started or self.game_over or event.keysym.lower() not in ("w", "a", "s", "d"):
            return
        if self.turn != self.player_role:
            return
        moves = {"w": (0, -1), "s": (0, 1), "a": (-1, 0), "d": (1, 0)}
        dx, dy = moves[event.keysym.lower()]
        target = (self.player[0] + dx, self.player[1] + dy)
        if self.can_enter(target) and target != self.robot:
            self.player = target
            if self.player_role == "T" and self.player == self.key:
                self.has_key = True
            self.end_player_turn()

    def can_enter(self, cell):
        return 1 <= cell[0] <= COLS and 1 <= cell[1] <= ROWS and cell not in self.obstacles and cell not in self.secondary_obstacles

    def end_player_turn(self):
        if self.check_victory():
            return
        self.turn = "J" if self.player_role == "T" else "T"
        self.robot_move()
        if self.check_victory():
            return
        self.turn = self.player_role
        self.round_number += 1
        self.draw_world()
        self.update_status()

    def robot_move(self):
        if self.player_role == "T":
            self.robot = self.step_toward(self.robot, self.player)
        else:
            target = self.key if not self.robot_has_key else self.random_cell(BANK_CELLS)
            self.robot = self.step_toward(self.robot, target, avoid=self.player)
            if self.robot == self.key:
                self.robot_has_key = True

    def step_toward(self, start, target, avoid=None):
        pending = deque([(start, None)])
        visited = {start}
        while pending:
            current, first_step = pending.popleft()
            if current == target:
                return first_step or start
            for neighbor in self.neighbors(current):
                if neighbor in visited or neighbor == avoid or not self.can_enter(neighbor):
                    continue
                visited.add(neighbor)
                pending.append((neighbor, first_step or neighbor))

        choices = [cell for cell in self.neighbors(start) if self.can_enter(cell) and cell != avoid]
        return random.choice(choices) if choices else start

    def check_victory(self):
        if not self.game_started:
            return False
        t_position = self.robot if self.player_role == "J" else self.player
        if self.player_role == "J":
            robot_in_player_range = self.robot in self.neighbors_3x3(self.player)
            player_in_robot_range = self.player in self.neighbors_3x3(t_position)
            if robot_in_player_range or player_in_robot_range:
                self.finish("J 胜利：机器人进入了你的 3×3 范围，你获得了5M币！", reward_player=True)
                return True
        elif self.has_key and self.player in BANK_CELLS and self.robot not in self.neighbors_3x3(t_position):
            self.finish("T 胜利：你拿到钥匙并到达了 BANK，你获得了5M币！", reward_player=True)
            return True
        if self.player_role == "J" and self.robot_has_key:
            if self.robot in BANK_CELLS and self.player not in self.neighbors_3x3(t_position):
                self.finish("T 胜利：机器人拿到钥匙并到达了 BANK。")
                return True
        return False

    def neighbors_3x3(self, center):
        x, y = center
        return {(x + dx, y + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1) if 1 <= x + dx <= COLS and 1 <= y + dy <= ROWS}

    def finish(self, message, reward_player=False):
        self.game_over = True
        if reward_player:
            self.add_reward()
        self.status.config(text=message)
        messagebox.showinfo("游戏结束", message)

    def add_reward(self):
        try:
            data = json.loads(DATA_FILE.read_text(encoding="utf-8"))
            data["M币"] = int(data.get("M币", 0)) + 5
            DATA_FILE.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            return

    def update_status(self):
        if not self.game_started:
            return
        key_text = "已获得钥匙" if self.has_key else "未获得钥匙"
        self.status.config(text=f"第 {self.round_number} 局 | 当前：{self.turn} | {key_text} | WASD 移动")


if __name__ == "__main__":
    window = tk.Tk()
    EasyWorldApp(window)
    window.mainloop()
