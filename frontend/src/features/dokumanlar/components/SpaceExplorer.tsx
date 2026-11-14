"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";
import { FolderOpen, FolderPlus, MoreHorizontal, Plus, SearchX, Upload } from "lucide-react";
import { toast } from "sonner";
import { ConfirmDialog } from "@/components/common/ConfirmDialog";
import { EmptyState } from "@/components/common/EmptyState";
import { ErrorState } from "@/components/common/ErrorState";
import { ImageViewer } from "@/components/common/ImageViewer";
import { ListSkeleton } from "@/components/common/Skeletons";
import type { RowMenuItem } from "@/components/common/RowMenu";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ApiError } from "@/lib/api";
import { PURPOSE_EXTENSIONS } from "@/lib/files";
import { useUrlParams } from "@/lib/url-state";
import { useFolderContent, useRestoreItem, useTrashItem } from "../api";
import { useUploadQueue } from "../useUploadQueue";
import { contentUrl, copyFileLink, downloadFile, notifyRestored, openPdf } from "../utils";
import { Breadcrumb, type Crumb } from "./Breadcrumb";
import { ConflictDialog } from "./ConflictDialog";
import { GrantsDialog } from "./GrantsDialog";
import { ItemList, type Entry } from "./ItemList";
import { MoveDialog, type MoveTarget } from "./MoveDialog";
import { NameDialog, type NameTarget } from "./NameDialog";
import { SpaceSettingsDialog } from "./SpaceSettingsDialog";
import { UploadPanel } from "./UploadPanel";

const ACCEPT = PURPOSE_EXTENSIONS.DMS.map((e) => `.${e}`).join(",");

export function SpaceExplorer({ spaceId }: { spaceId: number }) {
  const params = useUrlParams();
  const folderParam = Number(params.get("klasor"));
  const folderId = Number.isInteger(folderParam) && folderParam > 0 ? folderParam : undefined;
  const vurgu = Number(params.get("vurgu"));
  const content = useFolderContent(spaceId, folderId);
  const upload = useUploadQueue();
  const trash = useTrashItem();
  const restore = useRestoreItem();

  const [nameTarget, setNameTarget] = useState<NameTarget | null>(null);
  const [moveTarget, setMoveTarget] = useState<MoveTarget | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Entry | null>(null);
  const [grantsOpen, setGrantsOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [viewer, setViewer] = useState<number | null>(null);
  const [dragging, setDragging] = useState(false);
  const input = useRef<HTMLInputElement>(null);

  const level = content.data?.space.myLevel;
  const canEdit = level === "EDIT" || level === "MANAGE";
  const canManage = level === "MANAGE";

  const grantsVisible = grantsOpen || (params.get("yetkiler") === "1" && canManage);
  const highlightId = Number.isInteger(vurgu) && vurgu > 0 ? vurgu : undefined;
  const hasContent = content.isSuccess;
  useEffect(() => {
    if (!hasContent || highlightId === undefined) return;
    const t = setTimeout(() => params.set({ vurgu: undefined }), 2000);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasContent, highlightId]);

  const entries: Entry[] = useMemo(
    () => [
      ...(content.data?.folders ?? []).map((row) => ({ type: "folder" as const, row })),
      ...(content.data?.files ?? []).map((row) => ({ type: "file" as const, row })),
    ],
    [content.data],
  );
  const images = useMemo(
    () =>
      (content.data?.files ?? [])
        .filter((f) => f.contentType.startsWith("image/"))
        .map((f) => ({ fileId: String(f.id), name: f.name })),
    [content.data],
  );

  if (content.isPending) return <ListSkeleton rows={5} />;
  if (content.isError) {
    if (content.error instanceof ApiError && content.error.status === 404) {
      return (
        <EmptyState
          icon={SearchX}
          message={content.error.message}
          action={{ label: "Dokümanlar'a dön", href: "/dokumanlar" }}
        />
      );
    }
    return <ErrorState error={content.error} onRetry={() => void content.refetch()} />;
  }

  const { space, breadcrumb } = content.data;
  const base = `/dokumanlar/${space.id}`;
  const crumbs: Crumb[] = [
    { label: "Dokümanlar", href: "/dokumanlar" },
    { label: space.name, href: base },
    ...breadcrumb.map((b) => ({ label: b.name, href: `${base}?klasor=${b.id}` })),
  ];

  function pickFiles() {
    input.current?.click();
  }

  function open(e: Entry) {
    if (e.type === "folder") {
      params.set({ klasor: String(e.row.id), vurgu: undefined });
    } else if (e.row.previewable) {
      if (e.row.contentType.startsWith("image/")) {
        setViewer(images.findIndex((i) => i.fileId === String(e.row.id)));
      } else openPdf(e.row.id);
    } else downloadFile(e.row.id);
  }

  function menuItems(e: Entry): RowMenuItem[] {
    const kind = e.type;
    const id = e.row.id;
    return [
      { label: "Aç", hidden: kind !== "folder", onSelect: () => open(e) },
      {
        label: "Önizle",
        hidden: !(e.type === "file" && e.row.previewable),
        onSelect: () => open(e),
      },
      { label: "İndir", hidden: kind !== "file", onSelect: () => downloadFile(id) },
      { label: "Bağlantıyı kopyala", hidden: kind !== "file", onSelect: () => void copyFileLink(id) },
      {
        label: "Yeniden adlandır",
        hidden: !canEdit,
        onSelect: () => setNameTarget({ mode: "rename", kind, id, name: e.row.name }),
      },
      { label: "Taşı", hidden: !canEdit, onSelect: () => setMoveTarget({ kind, id, name: e.row.name }) },
      { label: "Sil", hidden: !canEdit, destructive: true, onSelect: () => setDeleteTarget(e) },
    ];
  }

  async function confirmDelete() {
    const e = deleteTarget;
    if (!e) return;
    await trash.mutateAsync({ kind: e.type, id: e.row.id });
    toast.success("Çöp kutusuna taşındı.", {
      action: {
        label: "Geri Al",
        onClick: () => void restore.mutateAsync({ kind: e.type, id: e.row.id }).then(notifyRestored, () => {}),
      },
    });
  }

  const trashHref = `${base}/cop`;

  return (
    <div
      className="relative min-h-[60dvh]"
      onDragOver={(e) => {
        if (!canEdit || !e.dataTransfer.types.includes("Files")) return;
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setDragging(false);
      }}
      onDrop={(e) => {
        if (!canEdit) return;
        e.preventDefault();
        setDragging(false);
        upload.enqueue(Array.from(e.dataTransfer.files), space.id, folderId);
      }}
    >
      <div className="mb-4 space-y-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <Breadcrumb crumbs={crumbs} />
          <div className="flex items-center gap-2">
            {canEdit && (
              <>
                <Button className="hidden h-10 md:inline-flex" onClick={pickFiles}>
                  <Upload aria-hidden />
                  Yükle
                </Button>
                <Button
                  variant="outline"
                  className="hidden h-10 md:inline-flex"
                  onClick={() => setNameTarget({ mode: "create", spaceId: space.id, parentId: folderId })}
                >
                  <FolderPlus aria-hidden />
                  Yeni Klasör
                </Button>
              </>
            )}
            {canManage && (
              <Button variant="outline" className="h-11 md:h-10" onClick={() => setGrantsOpen(true)}>
                Yetkiler
              </Button>
            )}
            {canEdit && (
              <DropdownMenu>
                <DropdownMenuTrigger
                  aria-label="Diğer işlemler"
                  className="flex size-11 items-center justify-center rounded-lg border outline-none hover:bg-muted focus-visible:ring-3 focus-visible:ring-ring/50 md:size-10"
                >
                  <MoreHorizontal className="size-5" aria-hidden />
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="min-w-48">
                  <DropdownMenuItem className="min-h-11" render={<Link href={trashHref} />}>
                    Çöp Kutusu
                  </DropdownMenuItem>
                  {canManage && (
                    <DropdownMenuItem className="min-h-11" onClick={() => setSettingsOpen(true)}>
                      Alan Ayarları
                    </DropdownMenuItem>
                  )}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
          </div>
        </div>
        {space.description && <p className="text-[13px] text-muted-foreground">{space.description}</p>}
      </div>

      {entries.length === 0 ? (
        <EmptyState
          icon={FolderOpen}
          message="Bu klasör boş."
          action={canEdit ? { label: "Dosya Yükle", onClick: pickFiles } : undefined}
        />
      ) : (
        <ItemList entries={entries} highlightId={highlightId} onOpen={open} menuItems={menuItems} />
      )}

      {canEdit && (
        <DropdownMenu>
          <DropdownMenuTrigger
            aria-label="Ekle"
            className="fixed right-4 bottom-[calc(64px+env(safe-area-inset-bottom)+16px)] z-30 flex size-14 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-lg outline-none focus-visible:ring-3 focus-visible:ring-ring/50 md:hidden"
          >
            <Plus className="size-6" aria-hidden />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="min-w-48">
            <DropdownMenuItem className="min-h-11" onClick={pickFiles}>
              Dosya Yükle
            </DropdownMenuItem>
            <DropdownMenuItem
              className="min-h-11"
              onClick={() => setNameTarget({ mode: "create", spaceId: space.id, parentId: folderId })}
            >
              Klasör Oluştur
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      )}

      {dragging && (
        <div className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center rounded-xl border-2 border-dashed border-primary bg-background/80">
          <p className="text-[15px] font-medium text-primary">Dosyaları bu klasöre bırakın</p>
        </div>
      )}

      <input
        ref={input}
        type="file"
        multiple
        hidden
        accept={ACCEPT}
        onChange={(e) => {
          upload.enqueue(Array.from(e.target.files ?? []), space.id, folderId);
          e.target.value = "";
        }}
      />

      <UploadPanel items={upload.items} onRetry={upload.retry} onDismiss={upload.dismiss} />
      <ConflictDialog request={upload.conflict} />
      <NameDialog target={nameTarget} onClose={() => setNameTarget(null)} />
      <MoveDialog spaceId={space.id} target={moveTarget} onClose={() => setMoveTarget(null)} />
      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(o) => !o && setDeleteTarget(null)}
        title="Silmek istiyor musunuz?"
        description={
          deleteTarget?.type === "folder"
            ? `'${deleteTarget.row.name}' klasörü ve içindeki her şey çöp kutusuna taşınacak. 30 gün içinde geri yükleyebilirsiniz.`
            : `'${deleteTarget?.row.name ?? ""}' çöp kutusuna taşınacak.`
        }
        confirmLabel="Sil"
        destructive
        onConfirm={confirmDelete}
      />
      {canManage && (
        <>
          <GrantsDialog space={space} open={grantsVisible}
            onOpenChange={(o) => {
              setGrantsOpen(o);
              if (!o) params.set({ yetkiler: undefined });
            }} />
          <SpaceSettingsDialog space={space} open={settingsOpen} onOpenChange={setSettingsOpen} />
        </>
      )}
      <ImageViewer
        fileUrl={(id, download) => contentUrl(Number(id), download === true)}
        images={images}
        index={viewer}
        onIndexChange={setViewer}
        onClose={() => setViewer(null)}
      />
    </div>
  );
}
