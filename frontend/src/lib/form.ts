"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, type FieldValues, type Path, type UseFormProps } from "react-hook-form";
import type { z } from "zod";
import { ApiError } from "@/lib/api";
import { toastError } from "@/lib/query-client";

export function useApiForm<TIn extends FieldValues, TOut extends FieldValues = TIn>(
  schema: z.ZodType<TOut, TIn>,
  options?: Omit<UseFormProps<TIn, unknown, TOut>, "resolver">,
) {
  const form = useForm<TIn, unknown, TOut>({
    resolver: zodResolver(schema),
    mode: "onTouched",
    ...options,
  });

  function submit(onValid: (values: TOut) => Promise<unknown> | unknown) {
    return form.handleSubmit(
      async (values) => {
        try {
          await onValid(values);
        } catch (err) {
          if (err instanceof ApiError) {
            const fields = err.fieldErrors;
            for (const fe of fields) {
              form.setError(fe.field as Path<TIn>, { message: fe.message });
            }
            if (fields.length > 0) form.setFocus(fields[0].field as Path<TIn>);
          } else {
            toastError("Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.");
          }
        }
      },
      (errors) => {
        const first = Object.keys(errors)[0];
        if (first) form.setFocus(first as Path<TIn>);
      },
    );
  }

  return { ...form, submit };
}
